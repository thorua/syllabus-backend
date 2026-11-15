package co.edu.uam.syllabus.common;

import java.io.IOException;
import java.time.Duration;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Sirve la interfaz Angular compilada (classpath:/static, la copia la imagen Docker) desde el mismo
 * origen que la API. Sus rutas (/syllabus/…, /users…) se resuelven en el navegador: cualquier ruta
 * que no sea un archivo ni de la API devuelve index.html.
 */
@Configuration
public class InterfazWebConfig implements WebMvcConfigurer {

    private static final String UBICACION = "classpath:/static/";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // JS y CSS llevan un hash en el nombre: cada despliegue cambia el nombre, así que no caducan.
        registry.addResourceHandler("/*.js", "/*.css", "/media/**")
                .addResourceLocations(UBICACION)
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable());

        // index.html se revalida siempre: así cada despliegue se ve de inmediato.
        registry.addResourceHandler("/**")
                .addResourceLocations(UBICACION)
                .setCacheControl(CacheControl.noCache())
                .resourceChain(true)
                .addResolver(new RutasDeLaInterfaz());
    }

    private static final class RutasDeLaInterfaz extends PathResourceResolver {

        @Override
        protected Resource getResource(String ruta, Resource ubicacion) throws IOException {
            Resource archivo = ubicacion.createRelative(ruta);
            if (archivo.isReadable()) {
                return archivo;
            }
            // Una ruta de la API que no existe es un 404, no la página de la interfaz.
            return ruta.startsWith("api/") ? null : ubicacion.createRelative("index.html");
        }
    }
}
