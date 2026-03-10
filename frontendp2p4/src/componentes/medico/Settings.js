import React, { useState, useEffect, useContext } from 'react';
import '../../css/stylesheet.css';
import { AppContext } from '../../context/AppContext';

const diasSemana = ["Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"];

const MedicoSettings = () => {
    const { usuario } = useContext(AppContext);
    const [perfil, setPerfil] = useState(null);
    const [imagenPreview, setImagenPreview] = useState(null);
    const [imagenFile, setImagenFile] = useState(null);

    useEffect(() => {
        const fetchPerfil = async () => {
            const res = await fetch('http://localhost:8080/api/medicos/perfil', {
                headers: {
                    Authorization: `Bearer ${usuario?.token}`,
                },
            });
            const data = await res.json();
            setPerfil(data);
        };

        if (usuario?.token) fetchPerfil();
    }, [usuario]);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setPerfil({ ...perfil, [name]: value });
    };

    const handleHorarioChange = (index, campo, valor) => {
        const nuevosHorarios = [...perfil.horarios];
        nuevosHorarios[index][campo] = valor;
        setPerfil({ ...perfil, horarios: nuevosHorarios });
    };

    const handleImagenChange = (e) => {
        const file = e.target.files[0];
        if (file) {
            setImagenFile(file);
            setImagenPreview(URL.createObjectURL(file));
        }
    };

    const guardarPerfil = async (e) => {
        e.preventDefault();

        const formData = new FormData();
        formData.append("perfil", new Blob([JSON.stringify(perfil)], { type: "application/json" }));
        if (imagenFile) {
            formData.append("fotoPerfil", imagenFile);
        }

        const res = await fetch("http://localhost:8080/api/medicos/editar", {
            method: "POST",
            headers: {
                Authorization: `Bearer ${usuario?.token}`,
            },
            body: formData,
        });

        if (res.ok) {
            alert("Perfil actualizado correctamente.");
        } else {
            alert("Error al guardar el perfil.");
        }
    };

    if (!perfil) return <div className="doctor-settings-container">Cargando perfil...</div>;

    return (
        <div className="doctor-settings-container">
            <h2>Editar Perfil Médico</h2>
            <form onSubmit={guardarPerfil} encType="multipart/form-data">
                <div className="doctor-settings-form-row">
                    {/* Imagen */}
                    <div className="doctor-settings-form-col imagen-col">
                        <div className="doctor-settings-form-group">
                            <label className="doctor-settings-form-label">Foto de Perfil:</label>
                            <input type="file" accept="image/*" onChange={handleImagenChange} className="doctor-settings-form-input" />
                            {imagenPreview || perfil.imagen ? (
                                <img
                                    src={imagenPreview || `http://localhost:8080/images/perfiles/${perfil.imagen}`}
                                    alt="Foto de perfil"
                                    className="doctor-settings-img-preview"
                                />
                            ) : null}
                        </div>
                    </div>

                    {/* Datos */}
                    <div className="doctor-settings-form-col datos-col">
                        <div className="doctor-settings-form-group">
                            <label className="doctor-settings-form-label">Nombre:</label>
                            <input type="text" name="nombre" value={perfil.nombre} onChange={handleChange} className="doctor-settings-form-input" />
                        </div>
                        <div className="doctor-settings-form-group">
                            <label className="doctor-settings-form-label">Especialidad:</label>
                            <input type="text" name="especialidad" value={perfil.especialidad} onChange={handleChange} className="doctor-settings-form-input" />
                        </div>
                        <div className="doctor-settings-form-group">
                            <label className="doctor-settings-form-label">Ciudad:</label>
                            <input type="text" name="ciudad" value={perfil.ciudad} onChange={handleChange} className="doctor-settings-form-input" />
                        </div>
                        <div className="doctor-settings-form-group">
                            <label className="doctor-settings-form-label">Instalación:</label>
                            <input type="text" name="instalacion" value={perfil.instalacion} onChange={handleChange} className="doctor-settings-form-input" />
                        </div>
                        <div className="doctor-settings-form-group">
                            <label className="doctor-settings-form-label">Costo de Consulta:</label>
                            <input type="number" name="costoConsulta" value={perfil.costoConsulta} onChange={handleChange} className="doctor-settings-form-input" />
                        </div>
                        <div className="doctor-settings-form-group">
                            <label className="doctor-settings-form-label">Frecuencia (minutos):</label>
                            <input type="number" name="frecuencia" value={perfil.frecuencia} onChange={handleChange} className="doctor-settings-form-input" />
                        </div>
                    </div>
                    {/* Horarios */}
                    <div className="doctor-settings-form-group">
                        <label className="doctor-settings-form-label">Configurar Horarios Semanales:</label>
                        {diasSemana.map((dia, index) => (
                            <div className="doctor-settings-horario-row" key={index}>
                                <label>{dia}:</label>
                                <input
                                    type="time"
                                    value={perfil.horarios[index]?.horaInicio || ''}
                                    onChange={(e) => handleHorarioChange(index, "horaInicio", e.target.value)}
                                />
                                <input
                                    type="time"
                                    value={perfil.horarios[index]?.horaFin || ''}
                                    onChange={(e) => handleHorarioChange(index, "horaFin", e.target.value)}
                                />
                            </div>
                        ))}
                    </div>
                </div>

                <button type="submit" className="doctor-settings-btn">Guardar</button>
            </form>
        </div>
    );
};

export default MedicoSettings;
