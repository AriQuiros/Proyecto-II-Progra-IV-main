package org.example.backendp2p4.dto;
import java.util.List;

public class MedicoCardDTO extends MedicoBaseDTO {
    private String instalacion;
    private List<org.example.backendp2p4.dto.HorarioDTO> horarios;
    private Integer frecuencia;
    private String imagen;


    public String getInstalacion() { return instalacion;}

    public void setInstalacion(String instalacion) { this.instalacion = instalacion;}

    public List<org.example.backendp2p4.dto.HorarioDTO> getHorarios() {return horarios;}

    public void setHorarios(List<org.example.backendp2p4.dto.HorarioDTO> horarios) {this.horarios = horarios;}

    public Integer getFrecuencia() { return frecuencia;}

    public void setFrecuencia(Integer frecuencia) { this.frecuencia = frecuencia;}

    public String getImagen() {return imagen;
    }

    public void setImagen(String imagen) {this.imagen = imagen;}

}
