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

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.annotation.Nonnull;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiDocumentation {

  /**
   * Configures the base OpenAPI spec data.
   */
  @Bean
  public @Nonnull OpenAPI openAPI(@Value("${info.app.version:dev-local}") String version) {
    return new OpenAPI()
      .info(new Info()
        .version(version)
        .license(new License()
          .url("https://creativecommons.org/licenses/by/4.0")
          .name("Creative Commons Attribution 4.0 International"))
        .title("SimRailInformationTools (SIT) API")
        .description("""
          Test
          1234
          """))
      .servers(List.of(new Server()
        .description("Default Backend")
        .url("https://apis.simrail.tools")));
  }

  /**
   * Customizer for automatic problem responses for all error response codes (except 404).
   */
  @Bean
  public @Nonnull OpenApiCustomizer errorResponsesCustomizer() {
    return openAPI -> {
      // add the problem detail schema to the spec
      var problemDetailSchema = new ObjectSchema()
        .description("Problem description according to RFC 9457")
        .externalDocs(new ExternalDocumentation()
          .description("RFC 9457")
          .url("https://datatracker.ietf.org/doc/html/rfc9457"))
        .addProperty("type", new StringSchema()
          .description("A URI reference identifying the problem type"))
        .addProperty("status", new IntegerSchema()
          .description("The HTTP status code generated by the server for the problem"))
        .addProperty("title", new StringSchema()
          .description("A human-readable summary of the problem type"))
        .addProperty("detail", new StringSchema()
          .description("A human-readable explanation specific to the occurrence of the problem"))
        .addProperty("instance", new StringSchema()
          .description("A URI reference that identifies the specific occurrence of the problem"))
        .additionalProperties(new Schema<>()
          .description("Additional members to further narrow the core cause of the problem"));
      openAPI.getComponents().addSchemas("ProblemDetail", problemDetailSchema);

      // add the problem detail response to all error responses (status >= 400, < 600)
      // that don't have another content type explicitly provided (except for 404 as
      // no detailed description for a "not found" error is needed
      openAPI.getPaths()
        .values()
        .stream()
        .flatMap(pathItem -> pathItem.readOperations().stream())
        .forEach(operation -> {
          for (var responseEntry : operation.getResponses().entrySet()) {
            try {
              // check if the response has its content explicitly provided,
              // in this case we can skip it as we don't want to change the
              // explicitly provided content declaration
              var response = responseEntry.getValue();
              if (response.getContent() != null) {
                continue;
              }

              // apply the problem detail schema as response type if the response is an error response
              var responseCode = Integer.parseInt(responseEntry.getKey());
              if (responseCode >= 400 && responseCode != 404 && responseCode <= 599) {
                var responseType = new MediaType().schema(new Schema<>().$ref("#/components/schemas/ProblemDetail"));
                var responseContent = new Content().addMediaType("application/problem+json", responseType);
                response.setContent(responseContent);
              }
            } catch (NumberFormatException _) {
              // just ignore, the key of the responses map must not be a status code
            }
          }
        });
    };
  }
}
