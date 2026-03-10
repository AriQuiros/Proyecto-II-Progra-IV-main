import React, { useEffect, useState, useContext } from "react";
import { AppContext } from "../../context/AppContext";

const Historico = () => {
    const { usuario } = useContext(AppContext);
    const [citas, setCitas] = useState([]);
    const [estado, setEstado] = useState("");
    const [medico, setMedico] = useState("");

    const [mostrarNotaModal, setMostrarNotaModal] = useState(false);
    const [notaSeleccionada, setNotaSeleccionada] = useState("");

    useEffect(() => {
        if (!usuario?.token) return;

        const fetchCitas = async () => {
            const params = new URLSearchParams();
            if (estado) params.append("estado", estado);
            if (medico) params.append("medico", medico);

            try {
                const res = await fetch(`http://localhost:8080/api/pacientes/citas?${params.toString()}`, {
                    headers: {
                        Authorization: `Bearer ${usuario.token}`,
                    },
                });
                const data = await res.json();
                setCitas(data);
            } catch (error) {
                console.error("Error al cargar el historial de citas:", error);
            }
        };

        fetchCitas();
    }, [usuario, estado, medico]);

    const handleSubmit = (e) => {
        e.preventDefault();
        // No necesitas hacer nada aquí, ya que el useEffect se dispara por los filtros
    };

    return (
        <div className="historial-citas-container">
            <h2>Historial de Citas del Paciente</h2>

            <form onSubmit={handleSubmit} className="filtros-form">
                <select value={estado} onChange={(e) => setEstado(e.target.value)}>
                    <option value="">Todos</option>
                    <option value="PENDIENTE">Pendiente</option>
                    <option value="CONFIRMADA">Confirmada</option>
                    <option value="COMPLETADA">Completada</option>
                    <option value="CANCELADA">Cancelada</option>
                </select>
                <input
                    type="text"
                    value={medico}
                    onChange={(e) => setMedico(e.target.value)}
                    placeholder="Nombre del médico"
                />
                <button type="submit">Buscar</button>
            </form>

            <div className="lista-citas">
                {citas.length === 0 ? (
                    <p>No hay citas registradas.</p>
                ) : (
                    citas.map((cita) => (
                        <div key={cita.id} className="cita-card">
                            <div className="lista-citas1">
                                <p><strong>Médico:</strong> {cita.doctorNombre}</p>
                                <p><strong>Especialidad:</strong> {cita.especialidad}</p>
                                <p><strong>Instalación:</strong> {cita.instalacion} @ {cita.ciudad}</p>
                            </div>
                            <div className="lista-citas2">
                                <p><strong>Fecha:</strong> {new Date(cita.fechaHora).toLocaleDateString()}</p>
                                <p><strong>Hora:</strong> {new Date(cita.fechaHora).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</p>
                                <span className={`estado ${cita.estado.toLowerCase()}`}>{cita.estado}</span>
                                {(cita.estado === 'CONFIRMADA' || cita.estado === 'COMPLETADA') && cita.notas && (
                                    <div className="acciones-cita">
                                        <button
                                            className="btn-ver-nota"
                                            onClick={() => {
                                                setNotaSeleccionada(cita.notas);
                                                setMostrarNotaModal(true);
                                            }}
                                        >
                                            🔍 Ver Nota
                                        </button>
                                    </div>
                                )}
                            </div>
                        </div>
                    ))
                )}
            </div>

            {/* Modal para ver nota */}
            {mostrarNotaModal && (
                <div className="modal-nota-overlay">
                    <div className="modal-nota">
                        <h3>Nota de la cita</h3>
                        <div className="modal-ver-nota">{notaSeleccionada}</div>
                        <div className="modal-nota-botones">
                            <button onClick={() => setMostrarNotaModal(false)} className="btn-confirmar">Cerrar</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Historico;
