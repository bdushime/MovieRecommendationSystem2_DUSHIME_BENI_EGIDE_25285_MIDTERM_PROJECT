package com.example.signup.controller;

import com.example.signup.modal.Movie;
import com.example.signup.service.MovieService;
import com.example.signup.service.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/movies")
public class MovieController {

    private final MovieService movieService;
    private final PdfService pdfService;
    private final MessageSource messageSource;
    private static final Logger log = LoggerFactory.getLogger(MovieController.class);

    @Autowired
    public MovieController(MovieService movieService, PdfService pdfService,MessageSource messageSource) {
        this.movieService = movieService;
        this.pdfService = pdfService;
        this.messageSource = messageSource;
    }


    // Paginated endpoint that returns JSON for AJAX calls
    @GetMapping("/api/paginated")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getMoviesPaginatedApi(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Sort sort = Sort.by(direction.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Movie> moviePage = movieService.getAllMoviesPaginated(pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("movies", moviePage.getContent());
        response.put("currentPage", page);
        response.put("totalPages", moviePage.getTotalPages());
        response.put("totalItems", moviePage.getTotalElements());
        response.put("pageSize", size);
        response.put("sortBy", sortBy);
        response.put("direction", direction);

        return ResponseEntity.ok(response);
    }


    @GetMapping("/paginated")
    public String getMoviesPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            Model model) {

        Sort sort = Sort.by(direction.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Movie> moviePage = movieService.getAllMoviesPaginated(pageable);

        model.addAttribute("movies", moviePage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", moviePage.getTotalPages());
        model.addAttribute("totalItems", moviePage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("direction", direction);

        return "movies_paginated";
    }

    // Endpoint to search for a movie by ID
    @GetMapping("/search")
    public String searchMovie(@RequestParam("id") Long id, Model model) {
        Optional<Movie> movieOptional = movieService.getMovieById(id);

        if (movieOptional.isPresent()) {
            model.addAttribute("searchedMovie", movieOptional.get());
        } else {
            model.addAttribute("searchError",
                    messageSource.getMessage("movie.notFound", null, LocaleContextHolder.getLocale()));
        }

        List<Movie> movies = movieService.getAllMovies();
        model.addAttribute("movies", movies);

        return "admin_page";
    }

    // Update movie by ID
    @PostMapping("/update/{id}")
    public String updateMovie(@PathVariable Long id,
                              @RequestParam String name,
                              @RequestParam String description,
                              @RequestParam("imageFile") MultipartFile imageFile) throws IOException {
        String imageUrl = movieService.saveImage(imageFile);

        Movie updatedMovie = new Movie();
        updatedMovie.setName(name);
        updatedMovie.setDescription(description);
        updatedMovie.setImageUrl(imageUrl);

        movieService.updateMovie(id, updatedMovie);

        return "redirect:/admin_page";
    }

    // Endpoint to display movies for admin
    @GetMapping("/admin")
    public String getAllMoviesForAdmin(Model model) {
        List<Movie> movies = movieService.getAllMovies();
        model.addAttribute("movies", movies);
        return "admin_page";
    }

    // Endpoint to get movie by ID
    @GetMapping("/{id}")
    public String getMovieById(@PathVariable Long id, Model model) {
        Optional<Movie> movie = movieService.getMovieById(id);
        movie.ifPresent(m -> model.addAttribute("movie", m));
        return movie.isPresent() ? "movie_detail" : "404";
    }

    // Endpoint to delete a movie
    @PostMapping("/delete/{id}")
    public String deleteMovie(@PathVariable Long id) {
        movieService.deleteMovie(id);
        return "redirect:/admin_page";
    }

    // Endpoint to download movie details as PDF
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadMoviePdf(@PathVariable Long id) {
        try {
            Movie movie = movieService.getMovieById(id)
                    .orElseThrow(() -> new RuntimeException("Movie not found"));

            byte[] pdfContent = pdfService.generateMoviePdf(movie);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", movie.getName() + ".pdf");

            return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }
    }


    @PostMapping("/toggleRecommendation/{id}")
    public String toggleRecommendation(@PathVariable Long id) {
        movieService.toggleRecommendation(id);
        return "redirect:/admin_page";
    }

    @GetMapping("/recommended")
    @ResponseBody
    public List<Movie> getRecommendedMovies() {
        return movieService.getRecommendedMovies();
    }
}