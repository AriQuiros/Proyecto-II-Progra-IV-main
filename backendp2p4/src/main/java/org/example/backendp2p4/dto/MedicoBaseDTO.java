package org.example.backendp2p4.dto;

import java.util.List;

public abstract class MedicoBaseDTO {
    private Integer id;
    private String nombre;
    private String especialidad;
    private String ciudad;
    private Integer costoConsulta;
    private String estado;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public Integer getCostoConsulta() {
        return costoConsulta;
    }

    public void setCostoConsulta(Integer costoConsulta) {
        this.costoConsulta = costoConsulta;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
