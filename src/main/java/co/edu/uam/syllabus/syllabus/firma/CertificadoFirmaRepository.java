package co.edu.uam.syllabus.syllabus.firma;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CertificadoFirmaRepository extends JpaRepository<CertificadoFirma, UUID> {}
