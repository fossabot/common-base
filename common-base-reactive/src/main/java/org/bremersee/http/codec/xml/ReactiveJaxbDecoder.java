/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bremersee.http.codec.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.XMLEvent;
import org.bremersee.xml.JaxbContextBuilder;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.AbstractDecoder;
import org.springframework.core.codec.CodecException;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.codec.xml.XmlEventDecoder;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.xml.StaxUtils;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Decode from a bytes stream containing XML elements to a stream of {@code Object}s (POJOs).
 *
 * <p>The decoding parts are taken from {@link org.springframework.http.codec.xml.Jaxb2XmlDecoder}.
 *
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @author Christian Bremer
 */
@Validated
public class ReactiveJaxbDecoder extends AbstractDecoder<Object> {

  /**
   * The default value for JAXB annotations.
   *
   * @see XmlRootElement#name()
   * @see XmlRootElement#namespace()
   * @see XmlType#name()
   * @see XmlType#namespace()
   */
  private static final String JAXB_DEFAULT_ANNOTATION_VALUE = "##default";

  private final XmlEventDecoder xmlEventDecoder = new XmlEventDecoder();

  private final JaxbContextBuilder jaxbContextBuilder;

  private final String[] nameSpaces;

  /**
   * Instantiates a new reactive jaxb decoder.
   *
   * @param jaxbContextBuilder the jaxb context builder
   * @param nameSpaces         the name spaces
   */
  @SuppressWarnings("WeakerAccess")
  public ReactiveJaxbDecoder(
      final JaxbContextBuilder jaxbContextBuilder,
      final String... nameSpaces) {

    super(MimeTypeUtils.APPLICATION_XML, MimeTypeUtils.TEXT_XML);
    this.jaxbContextBuilder = jaxbContextBuilder;
    this.nameSpaces = nameSpaces;
  }

  @Override
  public boolean canDecode(final ResolvableType elementType, @Nullable final MimeType mimeType) {
    if (super.canDecode(elementType, mimeType)) {
      final Class<?> outputClass = elementType.getRawClass();
      return jaxbContextBuilder.supports(outputClass, nameSpaces);
    } else {
      return false;
    }
  }

  @Override
  public Mono<Object> decodeToMono(
      final Publisher<DataBuffer> inputStream,
      final ResolvableType elementType,
      @Nullable final MimeType mimeType,
      @Nullable final Map<String, Object> hints) {
    return decode(inputStream, elementType, mimeType, hints).singleOrEmpty();
  }

  @Override
  public Flux<Object> decode(
      final Publisher<DataBuffer> inputStream,
      final ResolvableType elementType,
      @Nullable final MimeType mimeType,
      @Nullable final Map<String, Object> hints) {

    final Class<?> outputClass = elementType.getRawClass();
    Assert.state(outputClass != null, "Unresolvable output class");

    final Flux<XMLEvent> xmlEventFlux = this.xmlEventDecoder.decode(
        inputStream, ResolvableType.forClass(XMLEvent.class), mimeType, hints);

    final QName typeName = toQName(outputClass);
    final Flux<List<XMLEvent>> splitEvents = split(xmlEventFlux, typeName);

    return splitEvents.map(events -> {
      final Object value = unmarshal(events, outputClass);
      LogFormatUtils.traceDebug(logger, traceOn -> {
        final String formatted = LogFormatUtils.formatValue(value, !traceOn);
        return Hints.getLogPrefix(hints) + "Decoded [" + formatted + "]";
      });
      return value;
    });
  }

  private Object unmarshal(final List<XMLEvent> events, final Class<?> outputClass) {
    try {
      final Unmarshaller unmarshaller = jaxbContextBuilder
          .buildJaxbContext(nameSpaces)
          .createUnmarshaller();
      final XMLEventReader eventReader = StaxUtils.createXMLEventReader(events);
      if (outputClass.isAnnotationPresent(XmlRootElement.class)) {
        return unmarshaller.unmarshal(eventReader);
      } else {
        JAXBElement<?> jaxbElement = unmarshaller.unmarshal(eventReader, outputClass);
        return jaxbElement.getValue();
      }
    } catch (UnmarshalException ex) {
      throw new DecodingException("Could not unmarshal XML to " + outputClass, ex);
    } catch (JAXBException ex) {
      throw new CodecException("Invalid JAXB configuration", ex);
    }
  }

  /**
   * Returns the qualified name for the given class, according to the mapping rules in the JAXB
   * specification.
   */
  private QName toQName(final Class<?> outputClass) {
    String localPart;
    String namespaceUri;

    if (outputClass.isAnnotationPresent(XmlRootElement.class)) {
      final XmlRootElement annotation = outputClass.getAnnotation(XmlRootElement.class);
      localPart = annotation.name();
      namespaceUri = annotation.namespace();
    } else if (outputClass.isAnnotationPresent(XmlType.class)) {
      final XmlType annotation = outputClass.getAnnotation(XmlType.class);
      localPart = annotation.name();
      namespaceUri = annotation.namespace();
    } else {
      throw new IllegalArgumentException("Output class [" + outputClass.getName()
          + "] is neither annotated with @XmlRootElement nor @XmlType");
    }

    if (JAXB_DEFAULT_ANNOTATION_VALUE.equals(localPart)) {
      localPart = ClassUtils.getShortNameAsProperty(outputClass);
    }
    if (JAXB_DEFAULT_ANNOTATION_VALUE.equals(namespaceUri)) {
      final Package outputClassPackage = outputClass.getPackage();
      if (outputClassPackage != null && outputClassPackage.isAnnotationPresent(XmlSchema.class)) {
        final XmlSchema annotation = outputClassPackage.getAnnotation(XmlSchema.class);
        namespaceUri = annotation.namespace();
      } else {
        namespaceUri = XMLConstants.NULL_NS_URI;
      }
    }
    return new QName(namespaceUri, localPart);
  }

  /**
   * Split a flux of {@link XMLEvent XMLEvents} into a flux of XMLEvent lists, one list for each
   * branch of the tree that starts with the given qualified name. That is, given the XMLEvents
   * shown {@linkplain XmlEventDecoder here}, and the {@code desiredName} "{@code child}", this
   * method returns a flux of two lists, each of which containing the events of a particular branch
   * of the tree that starts with "{@code child}".
   * <ol>
   * <li>The first list, dealing with the first branch of the tree:
   * <ol>
   * <li>{@link javax.xml.stream.events.StartElement} {@code child}</li>
   * <li>{@link javax.xml.stream.events.Characters} {@code foo}</li>
   * <li>{@link javax.xml.stream.events.EndElement} {@code child}</li>
   * </ol>
   * <li>The second list, dealing with the second branch of the tree:
   * <ol>
   * <li>{@link javax.xml.stream.events.StartElement} {@code child}</li>
   * <li>{@link javax.xml.stream.events.Characters} {@code bar}</li>
   * <li>{@link javax.xml.stream.events.EndElement} {@code child}</li>
   * </ol>
   * </li>
   * </ol>
   */
  private Flux<List<XMLEvent>> split(Flux<XMLEvent> xmlEventFlux, QName desiredName) {
    return xmlEventFlux.flatMap(new SplitFunction(desiredName));
  }

  private static class SplitFunction implements
      Function<XMLEvent, Publisher<? extends List<XMLEvent>>> {

    private final QName desiredName;

    @Nullable
    private List<XMLEvent> events;

    private int elementDepth = 0;

    private int barrier = Integer.MAX_VALUE;

    /**
     * Instantiates a new Split function.
     *
     * @param desiredName the desired name
     */
    SplitFunction(QName desiredName) {
      this.desiredName = desiredName;
    }

    @Override
    public Publisher<? extends List<XMLEvent>> apply(XMLEvent event) {
      if (event.isStartElement()) {
        if (this.barrier == Integer.MAX_VALUE) {
          QName startElementName = event.asStartElement().getName();
          if (this.desiredName.equals(startElementName)) {
            this.events = new ArrayList<>();
            this.barrier = this.elementDepth;
          }
        }
        this.elementDepth++;
      }
      if (this.elementDepth > this.barrier) {
        Assert.state(this.events != null, "No XMLEvent List");
        this.events.add(event);
      }
      if (event.isEndElement()) {
        this.elementDepth--;
        if (this.elementDepth == this.barrier) {
          this.barrier = Integer.MAX_VALUE;
          Assert.state(this.events != null, "No XMLEvent List");
          return Mono.just(this.events);
        }
      }
      return Mono.empty();
    }
  }

}
