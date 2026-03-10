import React, { useState, useEffect, useContext } from 'react';
import '../../css/stylesheet.css';
import doctorDefault from '../../imagenes/usuario.png';
import { AppContext } from '../../context/AppContext';
import { useNavigate } from 'react-router-dom';

const BuscarMedicos = () => {
    const navigate = useNavigate();

    const [especialidad, setEspecialidad] = useState('');
    const [ciudad, setCiudad] = useState('');
    const [medicos, setMedicos] = useState([]);

    const { loading } = useContext(AppContext);

    useEffect(() => {
        const esp = localStorage.getItem("especialidad") || '';
        const ciu = localStorage.getItem("ciudad") || '';
        setEspecialidad(esp);
        setCiudad(ciu);

        fetch(`http://localhost:8080/api/medicos?especialidad=${esp}&ciudad=${ciu}`)
            .then(res => res.json())
            .then(data => setMedicos(data))
            .catch(err => console.error("Error al cargar médicos:", err));
    }, []);

    const handleBuscar = async (e) => {
        e.preventDefault();
        localStorage.setItem("especialidad", especialidad);
        localStorage.setItem("ciudad", ciudad);

        try {
            const res = await fetch(`http://localhost:8080/api/medicos?especialidad=${especialidad}&ciudad=${ciudad}`);
            const data = await res.json();
            setMedicos(data);
        } catch (error) {
            console.error("Error al buscar médicos:", error);
        }
    };

    if (loading) return <p style={{ textAlign: "center" }}>Cargando...</p>;

    return (
        <div className="search-contenido-doctor">
            <section className="search-bar-doctor">
                <form onSubmit={handleBuscar}>
                    <input type="text" placeholder="Speciality" value={especialidad} onChange={(e) => setEspecialidad(e.target.value)} />
                    <input type="text" placeholder="City" value={ciudad} onChange={(e) => setCiudad(e.target.value)} />
                    <button type="submit">Search</button>
                </form>
            </section>

            <section className="cards-doctor">
                {medicos.length === 0 ? (
                    <p style={{ textAlign: 'center', marginTop: '20px' }}>No se encontraron médicos disponibles.</p>
                ) : (
                    medicos.map((medico) => (
                        <div key={medico.id} className="medico-card-doctor">
                            <div className="card-left-doctor">
                                <img
                                    src={medico.imagen ? `http://localhost:8080/images/perfiles/${medico.imagen}` : doctorDefault}
                                    alt={medico.nombre}
                                    className="doctor-img-doctor"
                                />
                                <div>
                                    <div className="h-doctor">
                                        <h4>{medico.nombre}</h4>
                                        <p>
                                            {new Intl.NumberFormat('es-CR', {
                                                style: 'currency',
                                                currency: 'CRC'
                                            }).format(medico.costoConsulta)}
                                        </p>
                                    </div>
                                    <p>{medico.especialidad}</p>
                                    <p>{medico.instalacion} @ {medico.ciudad}</p>
                                </div>
                            </div>

                            <div className="card-right-doctor">
                                {medico.horarios?.map((horario, idx) => (
                                    <div key={idx} className="horario-group-doctor">
                                        <p className="pfecha">{horario.diaSemana}</p>
                                        <p className="pfecha">{horario.fechaReal}</p>
                                        <div className="horas-doctor">
                                            {horario.ocupado ? (
                                                <button className="hora-ocupada" disabled title="No disponible">
                                                    {horario.horaInicio}
                                                </button>
                                            ) : (
                                                <button
                                                    className="hora-libre"
                                                    onClick={() => {
                                                        const usuario = JSON.parse(localStorage.getItem("usuario"));
                                                        const query = `medicoId=${medico.id}&fecha=${horario.fechaReal}&hora=${horario.horaInicio}`;

                                                        if (!usuario) {
                                                            localStorage.setItem("previourl", `/confirmar-cita?${query}`);
                                                            navigate("/login");
                                                        } else if (usuario.rol === "PACIENTE") {
                                                            navigate(`/confirmar-cita?${query}`);
                                                        } else if (usuario.rol === "MEDICO") {
                                                            navigate("/medico/MedicoPanel");
                                                        } else if (usuario.rol === "ADMINISTRADOR") {
                                                            navigate("/admin/AdminPanel");
                                                        }
                                                    }}

                                                >
                                                    {horario.horaInicio}
                                                </button>
                                            )}
                                        </div>
                                    </div>
                                ))}
                            </div>

                            <div className="boton-doctor">
                                <button onClick={() => window.location.href = `/cronograma/${medico.id}`}>
                                    Schedule <i className="bi bi-arrow-right-circle-fill"></i>
                                </button>
                            </div>
                        </div>
                    ))
                )}
            </section>
        </div>
    );
};

export default BuscarMedicos;
