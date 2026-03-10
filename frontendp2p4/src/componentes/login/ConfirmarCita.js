import React, { useEffect, useState, useContext } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import doctorDefault from '../../imagenes/usuario.png';
import '../../css/stylesheet.css';
import { AppContext } from '../../context/AppContext';

const ConfirmarCita = () => {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const { usuario } = useContext(AppContext);

    const [medico, setMedico] = useState(null);
    const fecha = searchParams.get('fecha');
    const hora = searchParams.get('hora');
    const medicoId = searchParams.get('medicoId');

    useEffect(() => {
        if (!usuario || usuario.rol !== "PACIENTE") {
            navigate("/login");
            return;
        }

        const fetchMedico = async () => {
            const res = await fetch(`http://localhost:8080/api/medicos/${medicoId}`);
            if (res.ok) {
                const data = await res.json();
                setMedico(data);
            }
        };

        fetchMedico();
    }, [usuario, medicoId, navigate]);

    const confirmar = async () => {
        try {
            const res = await fetch("http://localhost:8080/api/pacientes/confirmarcita", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${usuario.token}`,
                },
                body: JSON.stringify({ medicoId, fecha, hora }),
            });

            if (res.ok) {
                alert("La cita fue reservada correctamente.");
                navigate("/paciente/Historico");
            } else {
                alert("Error al guardar la cita");
            }
        } catch (e) {
            console.error(e);
        }
    };

    if (!medico) return <p>Cargando datos del médico...</p>;

    return (
        <section className="cita-confirm-wrapper">
            <div className="cita-confirm-card">
                <img
                    src={medico.imagen ? `http://localhost:8080/images/perfiles/${medico.imagen}` : doctorDefault}
                    alt="Foto"
                    className="doctor-img-doctor"
                />
                <div className="cita-confirm-info">
                    <p><strong>{medico.nombre}</strong></p>
                    <p>{fecha} - {hora}</p>
                    <p>{medico.instalacion} @ {medico.ciudad}</p>
                </div>
                <div className="cita-confirm-botones">
                    <button onClick={confirmar} className="btn-confirmar">Confirmar</button>
                    <button onClick={() => navigate("/")} className="btn-cancelar">Cancelar</button>
                </div>
            </div>
        </section>
    );
};

export default ConfirmarCita;
