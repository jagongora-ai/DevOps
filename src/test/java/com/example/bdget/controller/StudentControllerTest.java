package com.example.bdget.controller;

import com.example.bdget.model.Student;
import com.example.bdget.service.StudentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StudentController.class)
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StudentService service;

    @Autowired
    private ObjectMapper mapper;

    private Student student;

    @BeforeEach
    void setUp() {
        student = new Student();
        student.setId(1L);
        student.setName("Maria");
    }

    @Test
    void testGetAllStudents() throws Exception {
        when(service.getAllStudents()).thenReturn(Arrays.asList(student));
        mockMvc.perform(get("/students"))
               .andExpect(status().isOk())
               .andExpect(content().json(mapper.writeValueAsString(Arrays.asList(student))));
    }

    @Test
    void testGetStudentById_found() throws Exception {
        when(service.getStudentById(1L)).thenReturn(Optional.of(student));
        mockMvc.perform(get("/students/1"))
               .andExpect(status().isOk())
               .andExpect(content().json(mapper.writeValueAsString(student)));
    }

    @Test
    void testGetStudentById_notFound() throws Exception {
        when(service.getStudentById(99L)).thenReturn(Optional.empty());
        mockMvc.perform(get("/students/99"))
               .andExpect(status().isNotFound());
    }

    @Test
    void testCreateStudent() throws Exception {
        when(service.createStudent(any(Student.class))).thenReturn(student);
        mockMvc.perform(post("/students")
               .contentType(MediaType.APPLICATION_JSON)
               .content(mapper.writeValueAsString(student)))
               .andExpect(status().isCreated())
               .andExpect(content().json(mapper.writeValueAsString(student)));
    }

    @Test
    void testUpdateStudent_found() throws Exception {
        when(service.updateStudent(eq(1L), any(Student.class))).thenReturn(student);
        mockMvc.perform(put("/students/1")
               .contentType(MediaType.APPLICATION_JSON)
               .content(mapper.writeValueAsString(student)))
               .andExpect(status().isOk())
               .andExpect(content().json(mapper.writeValueAsString(student)));
    }

    @Test
    void testUpdateStudent_notFound() throws Exception {
        when(service.updateStudent(eq(99L), any(Student.class))).thenReturn(null);
        mockMvc.perform(put("/students/99")
               .contentType(MediaType.APPLICATION_JSON)
               .content(mapper.writeValueAsString(student)))
               .andExpect(status().isNotFound());
    }

    @Test
    void testGetAllStudents_databaseDown_returns503() throws Exception {
        when(service.getAllStudents())
            .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("MySQL caído"));
        mockMvc.perform(get("/students"))
               .andExpect(status().isServiceUnavailable())
               .andExpect(jsonPath("$.status").value(503))
               .andExpect(jsonPath("$.message").value("Base de datos no disponible"));
    }

    @Test
    void testDeleteStudent() throws Exception {
        mockMvc.perform(delete("/students/1"))
               .andExpect(status().isNoContent());
        verify(service).deleteStudent(1L);
    }

    // ── Validación y errores HTTP (cubren GlobalExceptionHandler) ──

    @Test
    void testCreateStudent_invalidName_returns400() throws Exception {
        String invalid = "{\"name\":\"X1\"}";   // contiene números: viola @Pattern
        mockMvc.perform(post("/students")
               .contentType(MediaType.APPLICATION_JSON)
               .content(invalid))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.status").value(400))
               .andExpect(jsonPath("$.errors.name").exists());
        verifyNoInteractions(service);
    }

    @Test
    void testCreateStudent_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/students")
               .contentType(MediaType.APPLICATION_JSON)
               .content("{esto no es json"))
               .andExpect(status().isBadRequest());
    }

    @Test
    void testUnknownRoute_returns404_not500() throws Exception {
        // Un 404 no debe contarse como error de servidor (5XX) en CloudWatch
        mockMvc.perform(get("/ruta-que-no-existe"))
               .andExpect(status().isNotFound());
    }

    @Test
    void testMethodNotAllowed_returns405() throws Exception {
        mockMvc.perform(patch("/students/1"))
               .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void testBusinessRuleError_returns400() throws Exception {
        when(service.getAllStudents()).thenThrow(new IllegalStateException("regla de negocio"));
        mockMvc.perform(get("/students"))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.message").value("regla de negocio"));
    }
}
