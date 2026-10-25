package com.zincoid.nullbot.module.render;

import com.zincoid.nullbot.module.render.svg.SvgRasterizer;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * JTE SVG 渲染器：宿主与插件共用模板渲染和 PNG 输出管线。
 * <p>JTE SVG renderer shared by host and plugin template rendering and PNG output.</p>
 */
@Slf4j
@Component
public class JteSvgRenderer implements PluginRenderer {

    private static final String GENERATED_PACKAGE = "gg.jte.generated.";

    private final TemplateEngine engine;
    private final SvgRasterizer rasterizer;

    /**
     * 创建宿主 JTE SVG 渲染器。
     * <p>Creates the host JTE SVG renderer.</p>
     *
     * @param rasterizer the SVG rasterizer
     */
    @Autowired
    public JteSvgRenderer(SvgRasterizer rasterizer) {
        this(createEngine(), rasterizer);
    }

    private JteSvgRenderer(TemplateEngine engine, SvgRasterizer rasterizer) {
        this.engine = engine;
        this.rasterizer = rasterizer;
    }

    /**
     * 为插件创建独立的 JTE 图片渲染器。
     * <p>Creates an independent JTE image renderer for a plugin.</p>
     *
     * @param loader     the plugin classloader
     * @param jar        the plugin jar containing precompiled template classes
     * @param rasterizer the SVG rasterizer
     * @return the plugin image renderer
     */
    public static JteSvgRenderer forPlugin(ClassLoader loader, Path jar, SvgRasterizer rasterizer) {
        JteSvgRenderer renderer = new JteSvgRenderer(createEngine(loader, jar), rasterizer);
        renderer.registerBundledFonts(jar);
        return renderer;
    }

    @Override
    public byte[] svg(String svg) {
        return rasterizer.png(svg);
    }

    @Override
    public byte[] template(String template, Map<String, Object> model) {
        if (template == null || template.isBlank()) throw new RenderException("Missing key: template");
        try {
            StringOutput output = new StringOutput();
            engine.render(template, model == null ? Map.of() : model, output);
            return rasterizer.png(output.toString());
        } catch (RenderException e) {
            throw e;
        } catch (RuntimeException | LinkageError e) {
            throw new RenderException("Failed to render template: " + template, e);
        }
    }

    @Override
    public boolean font(byte[] font) {
        return FontRegistry.register(font);
    }

    // ════ Helpers ═════════════════

    private void registerBundledFonts(Path jar) {
        try (JarFile file = new JarFile(jar.toFile())) {
            Enumeration<JarEntry> entries = file.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory() || !name.startsWith("fonts/")) continue;
                String lower = name.toLowerCase(Locale.ROOT);
                if (!lower.endsWith(".ttf") && !lower.endsWith(".otf")) continue;
                try (InputStream in = file.getInputStream(entry)) {
                    font(in.readAllBytes());
                } catch (RuntimeException e) {
                    log.warn("Plugin font registration failed: {}", name, e);
                }
            }
        } catch (IOException e) {
            log.warn("Cannot scan plugin fonts: {}", jar, e);
        }
    }

    private static TemplateEngine createEngine() {
        return TemplateEngine.createPrecompiled(ContentType.Plain);
    }

    private static TemplateEngine createEngine(ClassLoader loader, Path jar) {
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(new PluginTemplateClassLoader(loader, jar));
            return TemplateEngine.createPrecompiled(ContentType.Plain);
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    /**
     * 插件模板类加载器：优先从插件 JAR 读取 JTE 生成类，避免与宿主同名模板冲突。
     * <p>Plugin template classloader: reads JTE generated classes from the plugin jar first,
     * avoiding name clashes with host templates.</p>
     */
    private static final class PluginTemplateClassLoader extends ClassLoader {

        private final Path jar;

        private PluginTemplateClassLoader(ClassLoader loader, Path jar) {
            super(loader);
            this.jar = jar;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!name.startsWith(GENERATED_PACKAGE)) return super.loadClass(name, resolve);
            synchronized (getClassLoadingLock(name)) {
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null) {
                    byte[] bytes = read(name);
                    if (bytes != null) loaded = defineClass(name, bytes, 0, bytes.length);
                }
                if (loaded != null) {
                    if (resolve) resolveClass(loaded);
                    return loaded;
                }
            }
            return super.loadClass(name, resolve);
        }

        private byte[] read(String name) throws ClassNotFoundException {
            try (JarFile file = new JarFile(jar.toFile())) {
                JarEntry entry = file.getJarEntry(name.replace('.', '/') + ".class");
                if (entry == null) return null;
                try (InputStream in = file.getInputStream(entry)) {
                    return in.readAllBytes();
                }
            } catch (IOException e) {
                throw new ClassNotFoundException("Cannot read template class: " + name, e);
            }
        }
    }
}
