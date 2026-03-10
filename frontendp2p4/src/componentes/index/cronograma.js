import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import '../../css/stylesheet.css';
import doctorDefault from '../../imagenes/usuario.png';

const Cronograma = () => {
    const { id } = useParams();
    const navigate = useNavigate();

    const [medico, setMedico] = useState(null);
    const [prevId, setPrevId] = useState(null);
    const [nextId, setNextId] = useState(null);

    useEffect(() => {
        (async () => {
            try {
                const res = await fetch(`http://localhost:8080/api/medicos/cronograma/${id}`);
                if (!res.ok) throw new Error('No se pudo cargar el cronograma');
                const data = await res.json();
                setMedico(data.medico);
                setPrevId(data.prevId);
                setNextId(data.nextId);
            } catch (err) {
                console.error("Error al cargar cronograma:", err);
            }
        })();
    }, [id]);

    if (!medico) return <p style={{ textAlign: 'center' }}>Cargando cronograma...</p>;

    return (
        <section className="cronograma-section">
            <div>
                <div className="cronograma-card">
                    <div className="cronograma-card-left">
                        <img
                            src={medico.imagen ? `http://localhost:8080/images/perfiles/${medico.imagen}` : doctorDefault}
                            alt={medico.nombre}
                            className="cronograma-doctor-img"
                        />
                        <div>
                            <div className="h">
                                <h4>{medico.nombre}</h4>
                                <p>{new Intl.NumberFormat('es-CR', {
                                    style: 'currency', currency: 'CRC'
                                }).format(medico.costoConsulta)}</p>
                            </div>
                            <p>{medico.especialidad}</p>
                            <p>{medico.instalacion} @ {medico.ciudad}</p>
                        </div>
                    </div>

                    <div className="cronograma-card-right">
                        {medico.horarios.map((h, idx) => (
                            <div key={idx} className="cronograma-horario-group">
                                <p className="pfecha">{h.diaSemana}</p>
                                <p className="pfecha">{h.fechaReal}</p>
                                <div className="cronograma-horas">
                                    {h.ocupado ? (
                                        <button className="btn-hora-ocupada" disabled>{h.horaInicio}</button>
                                    ) : (
                                        <button
                                            className="btn-hora-libre"
                                            onClick={() => {
                                                const usuario = JSON.parse(localStorage.getItem("usuario"));
                                                const query = `medicoId=${medico.id}&fecha=${h.fechaReal}&hora=${h.horaInicio}`;

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
                                            {h.horaInicio}
                                        </button>
                                    )}
                                </div>
                            </div>
                        ))}
                    </div>
                </div>

                <div className="cronograma-botones">
                    <button onClick={() => navigate(`/cronograma/${prevId}`)}>
                        Prev <i className="bi bi-arrow-left-circle-fill"></i>
                    </button>
                    <button onClick={() => navigate(`/cronograma/${nextId}`)}>
                        Next <i className="bi bi-arrow-right-circle-fill"></i>
                    </button>
                </div>

                <button className="cronograma-back-btn" onClick={() => navigate("/")}>Go Back</button>
            </div>
        </section>
    );
};

export default Cronograma;
