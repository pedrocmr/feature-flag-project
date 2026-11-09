package com.example.featureflags.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.context.annotation.Configuration;
import java.net.URI;
import java.nio.file.*;
import java.io.*;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/v1")
public class FeatureFlagController {
    public record Flag(String key, String name, String description, boolean enabled, Instant createdAt, Instant updatedAt) implements Serializable {}
    public record Create(@NotBlank @Pattern(regexp="[a-z][a-z0-9-]{0,63}") String key, @NotBlank @Size(max=100) String name, @Size(max=500) String description, @NotNull Boolean enabled) {}
    public record Update(@NotBlank @Size(max=100) String name, @Size(max=500) String description, @NotNull Boolean enabled) {}
    public record Toggle(@NotNull Boolean enabled) {}
    private final Map<String, Flag> flags = new TreeMap<>();
    private final Path file;
    public FeatureFlagController(@Value("${flags.storage-path:./data/flags.bin}") String path) throws IOException, ClassNotFoundException {
        file = Path.of(path).toAbsolutePath();
        if (Files.exists(file)) {
            try (var input = new ObjectInputStream(Files.newInputStream(file))) {
                for (Object item : (List<?>) input.readObject()) { Flag flag = (Flag)item; flags.put(flag.key(), flag); }
            }
        } else {
            Instant now = Instant.now();
            flags.put("welcome-banner", new Flag("welcome-banner", "Banner de boas-vindas", "Exibe uma mensagem na funcionalidade de exemplo", false, now, now));
            flags.put("special-offers", new Flag("special-offers", "Ofertas especiais", "Exibe ofertas na demonstração", false, now, now));
            save();
        }
    }
    private void save() throws IOException {
        Files.createDirectories(file.getParent());
        Path temp = Files.createTempFile(file.getParent(), "flags-", ".tmp");
        try {
            try (var out = new ObjectOutputStream(Files.newOutputStream(temp))) { out.writeObject(new ArrayList<>(flags.values())); }
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temp); }
    }
    private Flag require(String key) {
        Flag flag = flags.get(key);
        if (flag == null) throw new ApiException(404, "Feature flag não encontrada")
        return flag;
    }
    private void persist(String key, Flag value) throws IOException {
        Flag previous = flags.get(key);
        if (value == null) flags.remove(key); else flags.put(key,value);
        try { save(); } catch (IOException e) { if (previous == null) flags.remove(key); else flags.put(key,previous); throw e; }
    }
    @GetMapping("/flags") public synchronized List<Flag> list() { return List.copyOf(flags.values()); }
    @GetMapping("/flags/{key}") public synchronized Flag get(@PathVariable String key) { return require(key); }
    @PostMapping("/flags") public synchronized ResponseEntity<Flag> create(@Valid @RequestBody Create body) throws IOException {
        if (flags.containsKey(body.key())) throw new ApiException(409,"Chave já cadastrada");
        Instant now = Instant.now();
        Flag flag = new Flag(body.key(),body.name(),body.description(),body.enabled(),now,now);
        persist(flag.key(),flag);
        return ResponseEntity.created(URI.create("/api/v1/flags/"+flag.key())).body(flag);
    }
    @PutMapping("/flags/{key}") public synchronized Flag update(@PathVariable String key, @Valid @RequestBody Update body) throws IOException {
        Flag old = require(key);
        Flag flag = new Flag(key,body.name(),body.description(),body.enabled(),old.createdAt(),Instant.now());
        persist(key,flag); return flag;
    }
    @PatchMapping("/flags/{key}/status") public synchronized Flag toggle(@PathVariable String key, @Valid @RequestBody Toggle body) throws IOException {
        Flag old = require(key);
        return update(key,new Update(old.name(),old.description(),body.enabled()));
    }
    @DeleteMapping("/flags/{key}") public synchronized ResponseEntity<Void> delete(@PathVariable String key) throws IOException {
        require(key); persist(key,null); return ResponseEntity.noContent().build();
    }
    static class ApiException extends RuntimeException {
        final int status;
        ApiException(int status,String message) { super(message); this.status=status; }
    }
    @RestControllerAdvice
    static class Errors {
        @ExceptionHandler(ApiException.class) ResponseEntity<ProblemDetail> api(ApiException e) { return ResponseEntity.status(e.status).body(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(e.status), e.getMessage())); }
        @ExceptionHandler(IOException.class) ResponseEntity<ProblemDetail> storage(IOException e) { return ResponseEntity.internalServerError().body(ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,"Não foi possível persistir as flags")); }
        @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException e) {
            ProblemDetail problem=ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,"Dados inválidos");
            problem.setProperty("errors",e.getBindingResult().getFieldErrors().stream().map(error -> Map.of("field",error.getField(),"message",Objects.requireNonNullElse(error.getDefaultMessage(),"Inválido"))).toList());
            return ResponseEntity.badRequest().body(problem);
        }
    }
    @Configuration
    static class Cors implements WebMvcConfigurer {
        @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}") String[] origins;
        public void addCorsMappings(CorsRegistry registry) { registry.addMapping("/api/**").allowedOrigins(origins).allowedMethods("GET","POST","PUT","PATCH","DELETE","OPTIONS"); }
    }
}
