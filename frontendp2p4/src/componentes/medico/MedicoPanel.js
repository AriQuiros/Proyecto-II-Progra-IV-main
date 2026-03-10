import React, { useEffect, useState, useContext } from 'react';
import '../../css/stylesheet.css';
import { AppContext } from '../../context/AppContext';
import usuarioImg from '../../imagenes/usuario.png';

const MedicoPanel = () => {
    const { usuario } = useContext(AppContext);
    const [citas, setCitas] = useState([]);
    const [estado, setEstado] = useState('');
    const [paciente, setPaciente] = useState('');
    const [mensaje, setMensaje] = useState('');

    const [mostrarNotaModal, setMostrarNotaModal] = useState(false);
    const [notaTexto, setNotaTexto] = useState('');
    const [citaSeleccionada, setCitaSeleccionada] = useState(null);

    const [mostrarVerNotaModal, setMostrarVerNotaModal] = useState(false);
    const [notaParaVer, setNotaParaVer] = useState('');

    const fetchCitas = async () => {
        if (!usuario?.token) return;
        try {
            const query = new URLSearchParams();
            if (estado) query.append('estado', estado);
            if (paciente) query.append('paciente', paciente);

            const res = await fetch('http://localhost:8080/api/medicos/citas?' + query.toString(), {
                headers: {
                    Authorization: `Bearer ${usuario?.token}`,
                },
            });

            const data = await res.json();
            setCitas(data);
        } catch (err) {
            console.error('Error al cargar citas:', err);
        }
    };

    useEffect(() => {
      fetchCitas();
    }, [usuario, estado, paciente]);

    const confirmarCita = (id) => {
        setCitaSeleccionada(id);
        setNotaTexto('');
        setMostrarNotaModal(true);
    };

    const enviarNota = async () => {
        if (!notaTexto || !citaSeleccionada) return;

        const formData = new URLSearchParams();
        formData.append('citaId', citaSeleccionada);
        formData.append('notas', notaTexto);

        const res = await fetch('http://localhost:8080/api/medicos/confirmar', {
            method: 'POST',
            body: formData,
        });

        if (res.ok) {
            setMensaje('Cita confirmada');
            fetchCitas();
        } else {
            const errText = await res.text();
            console.error('Error al confirmar cita:', errText);
        }

        setMostrarNotaModal(false);
        setCitaSeleccionada(null);
    };

    const cancelarCita = async (id) => {
        if (!window.confirm("¿Estás seguro de que deseas cancelar esta cita?")) return;

        try {
            const res = await fetch(`http://localhost:8080/api/medicos/cancelar?citaId=${id}`, {
                method: 'GET',
                headers: {
                    Authorization: `Bearer ${usuario?.token}`,
                },
            });

            if (res.ok) {
                const texto = await res.text();
                setMensaje(texto);
                fetchCitas();
            } else {
                const error = await res.text();
                setMensaje(error || "Error al cancelar la cita.");
            }
        } catch (error) {
            console.error("Error de red al cancelar cita:", error);
            setMensaje("Error al conectar con el servidor.");
        }
    };


    return (
        <div className="doctor-citas-contenido">
            <h2>{usuario?.nombre} - Panel de Citas</h2>
            {mensaje && <p className="mensaje-exito">{mensaje}</p>}

            <form className="doctor-search-bar" onSubmit={(e) => { e.preventDefault(); fetchCitas(); }}>
                <select value={estado} onChange={(e) => setEstado(e.target.value)}>
                    <option value="">Estado</option>
                    <option value="PENDIENTE">Pendiente</option>
                    <option value="CONFIRMADA">Confirmada</option>
                    <option value="CANCELADA">Cancelada</option>
                </select>
                <input
                    type="text"
                    placeholder="Paciente"
                    value={paciente}
                    onChange={(e) => setPaciente(e.target.value)}
                />
                <button type="submit" className="doctor-btn-search">Buscar</button>
            </form>

            <div className="doctor-cards-container">
                {Array.isArray(citas) && citas.length > 0 ? (
                    citas.map((cita) => (
                        <div className="doctor-card-cita" key={cita.numero}>
                            <div className="doctor-left">
                                <img src={usuarioImg} alt="Paciente" />
                                <span className="doctor-paciente-nombre">{cita.pacienteNombre}</span>
                            </div>
                            <div className="doctor-right">
                                <p>{new Date(cita.fechaHora).toLocaleString()}</p>
                                <span className={`badge-${cita.estado.toLowerCase()}-doctor`}>
                                    {cita.estado}
                                </span>
                                {cita.estado === 'PENDIENTE' && (
                                    <div className="doctor-acciones">
                                        <button className="link-green-doctor" onClick={() => confirmarCita(cita.numero)}>✔ Atender</button>
                                        <button className="link-red-doctor" onClick={() => cancelarCita(cita.numero)}>✘ Cancelar</button>
                                    </div>
                                )}
                                {cita.estado === 'CONFIRMADA' && (
                                    <div className="doctor-acciones">
                                        <button
                                            className="link-view-doctor"
                                            onClick={() => {
                                                setNotaParaVer(cita.notas || "Sin notas");
                                                setMostrarVerNotaModal(true);
                                            }}
                                        >
                                            🔍 Ver Nota
                                        </button>
                                    </div>
                                )}
                            </div>
                        </div>
                    ))
                ) : (
                    <p style={{ marginTop: "1rem", textAlign: "center", color: "#555" }}>
                        No hay citas registradas en este momento.
                    </p>
                )}
            </div>

            {/* Modal para confirmar cita */}
            {mostrarNotaModal && (
                <div className="modal-nota-overlay">
                    <div className="modal-nota">
                        <h3>Ingrese las notas de la cita</h3>
                        <textarea
                            value={notaTexto}
                            onChange={(e) => setNotaTexto(e.target.value)}
                            placeholder="Notas..."
                            rows={5}
                        />
                        <div className="modal-nota-botones">
                            <button onClick={enviarNota} className="btn-confirmar">Confirmar</button>
                            <button onClick={() => setMostrarNotaModal(false)} className="btn-cancelar">Cancelar</button>
                        </div>
                    </div>
                </div>
            )}

            {/* Modal para ver nota */}
            {mostrarVerNotaModal && (
                <div className="modal-nota-overlay">
                    <div className="modal-nota">
                        <h3>Nota de la cita</h3>
                        <div className="modal-ver-nota">{notaParaVer}</div>
                        <div className="modal-nota-botones">
                            <button onClick={() => setMostrarVerNotaModal(false)} className="btn-confirmar">Cerrar</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default MedicoPanel;
