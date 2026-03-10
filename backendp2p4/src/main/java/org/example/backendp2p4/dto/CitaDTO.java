package org.example.backendp2p4.dto;
import java.time.LocalDateTime;

public class CitaDTO {
    private Integer numero;
    private String pacienteNombre;
    private LocalDateTime fechaHora;
    private String estado;
    private String notas;

    public Integer getNumero() { return numero; }

    public void setNumero(Integer numero) { this.numero = numero; }

    public String getPacienteNombre() {return pacienteNombre;}

    public void setPacienteNombre(String pacienteNombre) {this.pacienteNombre = pacienteNombre;}

    public LocalDateTime getFechaHora() {return fechaHora;}

    public void setFechaHora(LocalDateTime fechaHora) {this.fechaHora = fechaHora;}

    public String getEstado() {return estado;}

    public void setEstado(String estado) {this.estado = estado;}

    public String getNotas() {return notas;}

    public void setNotas(String notas) {this.notas = notas;}
}
