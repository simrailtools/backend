/*
 * This file is part of simrail-tools-backend, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2024-2025 Pasqual Koschmieder and contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package tools.simrail.backend.api.configuration;

import jakarta.annotation.Nonnull;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.simrail.backend.api.util.ETagGenerator;

@Configuration
class WebMvcConfiguration implements WebMvcConfigurer {

  private final ETagGenerator etagGenerator;

  @Autowired
  public WebMvcConfiguration(@Nonnull ETagGenerator etagGenerator) {
    this.etagGenerator = etagGenerator;
  }

  /**
   * Configures the resource handlers for the documentation pages.
   */
  @Override
  public void addResourceHandlers(@Nonnull ResourceHandlerRegistry registry) {
    registry
      .addResourceHandler("/docs/**")
      .addResourceLocations("classpath:/resources/docs/")
      .setCacheControl(CacheControl.maxAge(Duration.ofDays(1)))
      .setEtagGenerator(this.etagGenerator);
  }

  /**
   * Configures a redirect of {@code /docs} and {@code /docs/} to {@code /docs/index.html}.
   */
  @Override
  public void addViewControllers(@Nonnull ViewControllerRegistry registry) {
    registry.addRedirectViewController("/docs", "/docs/index.html");
    registry.addRedirectViewController("/docs/", "/docs/index.html");
  }
}
