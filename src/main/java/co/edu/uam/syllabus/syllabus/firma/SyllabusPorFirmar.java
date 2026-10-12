package co.edu.uam.syllabus.syllabus.firma;

import java.util.UUID;

/** Un syllabus vigente quedó esperando una firma: se avisa a quien le corresponde. */
public record SyllabusPorFirmar(UUID syllabusId) {}
