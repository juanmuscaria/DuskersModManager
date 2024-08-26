package com.juanmuscaria.tooling.dmm.file

import com.juanmuscaria.tooling.dmm.UnsafeJvm
import org.gradle.api.internal.resources.DefaultResourceHandler
import org.gradle.api.internal.resources.ResourceResolver
import org.gradle.api.resources.MissingResourceException
import org.gradle.api.resources.ResourceException
import org.gradle.api.resources.ResourceHandler
import org.gradle.api.resources.internal.ReadableResourceInternal
import org.jetbrains.annotations.NotNull
import org.tukaani.xz.SeekableFileInputStream
import org.tukaani.xz.SeekableXZInputStream

import java.lang.invoke.MethodHandle

class XZArchiver extends AbstractArchiver {
    static MethodHandle resourceResolver = UnsafeJvm.theLookup
            .unreflectGetter(DefaultResourceHandler.class.getDeclaredField("resourceResolver"))

    static ReadableResourceInternal xz(ResourceHandler handle, Object path) {
        var resolver = resourceResolver.invokeWithArguments(handle) as ResourceResolver
        return new XZArchiver(resolver.resolveResource(path))
    }

    XZArchiver(ReadableResourceInternal resource) {
        super(resource)
    }

    @Override
    protected String getSchemePrefix() {
        return "xz:"
    }

    @NotNull
    @Override
    InputStream read() throws MissingResourceException, ResourceException {
        return new SeekableXZInputStream(new SeekableFileInputStream(resource.backingFile))
    }
}
