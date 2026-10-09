package co.edu.uam.syllabus.usuarios;

import co.edu.uam.syllabus.catalogos.Departamento;
import co.edu.uam.syllabus.catalogos.Programa;
import co.edu.uam.syllabus.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

@Entity
@Table(name = "users")
public class Usuario extends BaseEntity {

    public enum Genero { MASCULINO, FEMENINO, OTRO }

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private Genero gender;

    private String phone;

    @Column(name = "student_code")
    private String studentCode;

    @Column(name = "employee_code")
    private String employeeCode;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @ElementCollection
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Set<Rol> roles = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "user_departments",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "department_id"))
    private Set<Departamento> departamentos = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "user_programs",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "program_id"))
    private Set<Programa> programas = new HashSet<>();

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PermisoOverride> overrides = new HashSet<>();

    /** Permiso efectivo = (permisos de los roles ∪ otorgados) − restringidos. */
    public Set<String> permisosEfectivos() {
        Set<String> permisos = new TreeSet<>();
        roles.forEach(r -> permisos.addAll(r.permisos()));
        overrides.stream().filter(PermisoOverride::isAllow).forEach(o -> permisos.add(o.permiso()));
        overrides.stream().filter(o -> !o.isAllow()).forEach(o -> permisos.remove(o.permiso()));
        return permisos;
    }

    /** Rol de mayor jerarquía; solo se usa para mostrar. */
    public Rol rolPrincipal() {
        return roles.stream().max(Comparator.comparingInt(Rol::nivel)).orElse(Rol.ESTUDIANTE);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Genero getGender() {
        return gender;
    }

    public void setGender(Genero gender) {
        this.gender = gender;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getStudentCode() {
        return studentCode;
    }

    public void setStudentCode(String studentCode) {
        this.studentCode = studentCode;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Set<Rol> getRoles() {
        return roles;
    }

    public Set<Departamento> getDepartamentos() {
        return departamentos;
    }

    public Set<Programa> getProgramas() {
        return programas;
    }

    public Set<PermisoOverride> getOverrides() {
        return overrides;
    }
}
