package org.example.backendp2p4.dto;

public class MedicoCronogramaDTO {
    private MedicoCardDTO medico;
    private Integer prevId;
    private Integer nextId;

    public MedicoCardDTO getMedico() {
        return medico;
    }

    public void setMedico(MedicoCardDTO medico) {
        this.medico = medico;
    }

    public Integer getPrevId() {
        return prevId;
    }

    public void setPrevId(Integer prevId) {
        this.prevId = prevId;
    }

    public Integer getNextId() {
        return nextId;
    }

    public void setNextId(Integer nextId) {
        this.nextId = nextId;
    }
}
