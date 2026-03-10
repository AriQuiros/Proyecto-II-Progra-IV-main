import React, { useEffect, useState, useContext } from 'react';
import '../../css/stylesheet.css';
import { AppContext } from '../../context/AppContext';
import usuarioImg from '../../imagenes/usuario.png';

const MedicoPerfil = () => {
    const { usuario } = useContext(AppContext);
    const [perfil, setPerfil] = useState(null);

    useEffect(() => {
        const fetchPerfil = async () => {
            try {
                const res = await fetch('http://localhost:8080/api/medicos/perfil', {
                    headers: {
                        Authorization: `Bearer ${usuario?.token}`,
                    },
                });
                if (!res.ok) throw new Error('Error al cargar perfil');
                const data = await res.json();
                setPerfil(data);
            } catch (err) {
                console.error('Error:', err);
            }
        };

        if (usuario?.token) fetchPerfil();
    }, [usuario]);

    if (!perfil) {
        return <div className="doctor-perfil-container">Cargando perfil...</div>;
    }

    return (
        <div className="doctor-perfil-container">
            <h2 className="doctor-perfil-title">Perfil del Dr. {perfil.nombre}</h2>
            <div className="doctor-perfil-body">
                <div className="doctor-perfil-foto">
                    {perfil.imagen ? (
                        <img src={`http://localhost:8080/images/perfiles/${perfil.imagen}`} alt="Foto de perfil" />
                    ) : (
                        <img src={usuarioImg} alt="Foto por defecto"/>
                    )}
                </div>
                <div className="doctor-perfil-info">
                    <p><strong>Nombre:</strong> {perfil.nombre}</p>
                    <p><strong>Especialidad:</strong> {perfil.especialidad}</p>
                    <p><strong>Ciudad:</strong> {perfil.ciudad}</p>
                    <p><strong>Instalación:</strong> {perfil.instalacion}</p>
                    <p><strong>Costo de Consulta:</strong> ₡{perfil.costoConsulta}</p>
                    <p><strong>Frecuencia de Citas:</strong> {perfil.frecuencia} min</p>
                    <p><strong>Horario de atención:</strong></p>
                    <ul className="doctor-perfil-horarios">
                        {perfil.horarios?.map((h, index) => (
                            <li key={index}>
                                {h.diaSemana}: {h.horaInicio} - {h.horaFin}
                            </li>
                        ))}
                    </ul>
                </div>
            </div>
        </div>
    );
};

export default MedicoPerfil;
