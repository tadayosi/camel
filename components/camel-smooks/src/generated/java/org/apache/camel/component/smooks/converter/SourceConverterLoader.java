/* Generated by camel build tools - do NOT edit this file! */
package org.apache.camel.component.smooks.converter;

import javax.annotation.processing.Generated;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.DeferredContextBinding;
import org.apache.camel.Exchange;
import org.apache.camel.TypeConversionException;
import org.apache.camel.TypeConverterLoaderException;
import org.apache.camel.spi.TypeConverterLoader;
import org.apache.camel.spi.TypeConverterRegistry;
import org.apache.camel.support.SimpleTypeConverter;
import org.apache.camel.support.TypeConverterSupport;
import org.apache.camel.util.DoubleMap;

/**
 * Generated by camel build tools - do NOT edit this file!
 */
@Generated("org.apache.camel.maven.packaging.TypeConverterLoaderGeneratorMojo")
@SuppressWarnings("unchecked")
@DeferredContextBinding
public final class SourceConverterLoader implements TypeConverterLoader, CamelContextAware {

    private CamelContext camelContext;

    public SourceConverterLoader() {
    }

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Override
    public CamelContext getCamelContext() {
        return camelContext;
    }

    @Override
    public void load(TypeConverterRegistry registry) throws TypeConverterLoaderException {
        registerConverters(registry);
    }

    private void registerConverters(TypeConverterRegistry registry) {
        addTypeConverter(registry, javax.xml.transform.Source.class, java.io.InputStream.class, false,
            (type, exchange, value) -> org.apache.camel.component.smooks.converter.SourceConverter.toStreamSource((java.io.InputStream) value));
        addTypeConverter(registry, javax.xml.transform.Source.class, org.apache.camel.WrappedFile.class, true,
            (type, exchange, value) -> org.apache.camel.component.smooks.converter.SourceConverter.toStreamSource((org.apache.camel.WrappedFile) value, exchange));
        addTypeConverter(registry, org.smooks.io.payload.JavaSource.class, java.lang.Object.class, false,
            (type, exchange, value) -> org.apache.camel.component.smooks.converter.SourceConverter.toJavaSource(value));
        addTypeConverter(registry, org.smooks.io.payload.JavaSource.class, org.smooks.io.payload.JavaResult.class, false,
            (type, exchange, value) -> org.apache.camel.component.smooks.converter.SourceConverter.toJavaSource((org.smooks.io.payload.JavaResult) value));
        addTypeConverter(registry, org.smooks.io.payload.JavaSourceWithoutEventStream.class, java.lang.Object.class, false,
            (type, exchange, value) -> org.apache.camel.component.smooks.converter.SourceConverter.toJavaSourceWithoutEventStream(value));
    }

    private static void addTypeConverter(TypeConverterRegistry registry, Class<?> toType, Class<?> fromType, boolean allowNull, SimpleTypeConverter.ConversionMethod method) {
        registry.addTypeConverter(toType, fromType, new SimpleTypeConverter(allowNull, method));
    }
}
