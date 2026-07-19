package com.alef.api.admin.upload;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves uploaded files back under the public /uploads/** prefix (architecture
 * §2.4, DEC-022). Read-only, no directory listing -- Spring's resource handler
 * only serves an exact matched path.
 *
 * /uploads/** is deliberately outside /api/admin/**, so it stays public and
 * unauthenticated (AdminSecurityConfig's anyRequest().permitAll() default) --
 * uploaded project/team/sample assets must be visible on the public site with
 * no auth (F12-AC20/AC21).
 */
@Configuration
public class UploadResourceConfig implements WebMvcConfigurer {

    private final String uploadDir;

    public UploadResourceConfig(@Value("${alef.upload-dir:/var/alef/uploads}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = uploadDir.endsWith("/") ? uploadDir : uploadDir + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + location);
    }
}
