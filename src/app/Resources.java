/*
 * Copyright (c) 2014, Aleksey Nikolaevich Dokshin. All right reserved.
 * Contacts: dant.it@gmail.com, dokshin@list.ru.
 */
package app;

import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/**
 * Реализация общего хранилища ресурсов с ленивой инициализацией.
 *
 * @author Докшин Алексей Николаевич <dant.it@gmail.com>
 */
public class Resources {

    private static final HashMap<String, Lazy> lazy = new HashMap<>();
    private static final HashMap<String, Object> map = new HashMap<>();

    public static Object getObject(final String name) {
        return map.get(name);
    }

    public static <T> T getTyped(final String name) {
        return (T) map.get(name);
    }

    public static String getPath() {
        return "resources/";
    }

    public static URL getURL(String name) {
        return Resources.class.getResource(getPath() + name);
    }

    public static InputStream getAsStream(String name) {
        return Resources.class.getResourceAsStream(getPath() + name);
    }

    public static abstract class Lazy<T> {

        protected final String name;
        private T value;

        public Lazy(final String name) {
            this.name = name;
            this.value = null;
        }

        public synchronized T get() {
            if (value == null) {
                String key = getClass().getSimpleName() + ":" + name;
                value = (T) map.get(key);
                if (value == null) {
                    value = create();
                    map.put(key, value);
                }
            }
            return value;
        }

        public String getPath() {
            return ""; // По умолчанию - корень ресурсов.
        }

        public String getFullName() {
            return getPath() + name;
        }

        public URL getURL() {
            return Resources.getURL(getFullName());
        }

        protected abstract T create();
    }

    public static class LazyImage extends Lazy<Image> {

        public LazyImage(String name) {
            super(name);
        }

        @Override
        public String getPath() {
            return super.getPath() + "images/";
        }

        @Override
        protected Image create() {
            try {
                return ImageIO.read(getAsStream(getFullName()));
            } catch (IOException e) {
                return null;
            }
        }
    }

    public static class LazyImageIcon extends Lazy<ImageIcon> {

        public LazyImageIcon(String name) {
            super(name);
        }

        @Override
        public String getPath() {
            return super.getPath() + "images/";
        }

        @Override
        protected ImageIcon create() {
            try {
                return new ImageIcon(ImageIO.read(getAsStream(getFullName())));
            } catch (IOException e) {
                return null;
            }
        }
    }
}
