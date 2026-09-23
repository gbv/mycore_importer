package de.vzg.oai_importer.view;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Helper for building application relative URLs inside jte templates.
 * It replaces the Thymeleaf link expression {@code @{/path}}.
 */
public final class UrlHelper {

    private UrlHelper() {
    }

    /**
     * Checks whether the current request belongs to the given section of the application.
     * Used to mark the active entry in the main navigation.
     *
     * @param path the section path, starting with a slash
     * @return true if the current request path lies within the section
     */
    public static boolean isSection(String path) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return false;
        }

        String requestPath = attributes.getRequest().getRequestURI();
        String contextPath = attributes.getRequest().getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && requestPath.startsWith(contextPath)) {
            requestPath = requestPath.substring(contextPath.length());
        }

        return requestPath.startsWith(path);
    }

    /**
     * Prefixes an application absolute path with the servlet context path of the current request.
     *
     * @param path the path inside the application, starting with a slash
     * @return the path including the context path
     */
    public static String url(String path) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return path;
        }

        String contextPath = attributes.getRequest().getContextPath();
        if (contextPath == null || contextPath.isEmpty() || "/".equals(contextPath)) {
            return path;
        }

        return contextPath + path;
    }
}
