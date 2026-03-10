import React, { useEffect, useState, useContext } from 'react';
import '../../css/stylesheet.css';
import { AppContext } from '../../context/AppContext';

const AdminPanel = () => {
    const [doctores, setDoctores] = useState([]);
    const { usuario } = useContext(AppContext);
    const [mensaje, setMensaje] = useState('');

    useEffect(() => {
        if (usuario?.token) {
            (async () => {
                try {
                    const res = await fetch('http://localhost:8080/api/admin/doctores', {
                        headers: {
                            Authorization: `Bearer ${usuario?.token}`,
                        },
                    });

                    const data = await res.json();

                    if (!res.ok) {
                        console.error('Error al cargar doctores:', data);
                        return;
                    }

                    setDoctores(data);
                } catch (err) {
                    console.error('Error en fetchDoctores:', err);
                }
            })();
        }
    }, [usuario]);


    const aprobarMedico = async (id) => {
        try {
            const res = await fetch(`http://localhost:8080/api/admin/aprobarMedico/${id}`, {
                method: 'POST',
                headers: {
                    Authorization: `Bearer ${usuario?.token}`,
                },
            });

            if (res.ok) {
                setDoctores((prev) =>
                    prev.map((medico) =>
                        medico.id === id ? { ...medico, estado: 'APROBADO' } : medico
                    )
                );
                setMensaje('Médico aprobado correctamente');
            } else {
                const errorText = await res.text();
                console.error('Error al aprobar médico:', errorText);
                setMensaje('Error al aprobar médico');
            }
        } catch (error) {
            console.error('Error en aprobarMedico:', error);
            setMensaje('Error de conexión al aprobar médico');
        }
    };

    return (
        <div className="admin-contenido">
            <h1>Lista de Doctores</h1>
            {mensaje && <p className="mensaje-exito">{mensaje}</p>}
            <table className="admin-medico-table">
                <thead>
                <tr>
                    <th>Nombre</th>
                    <th>Especialidad</th>
                    <th>Ciudad</th>
                    <th>Costo de Consulta</th>
                    <th>Estado</th>
                    <th>Acciones</th>
                </tr>
                </thead>
                <tbody>
                {doctores.map((medico) => (
                    <tr key={medico.id}>
                        <td>{medico.nombre}</td>
                        <td>{medico.especialidad}</td>
                        <td>{medico.ciudad}</td>
                        <td>{medico.costoConsulta}</td>
                        <td>{medico.estado}</td>
                        <td>
                            {medico.estado === 'PENDIENTE' && (
                                <button
                                    className="btn-aprobar"
                                    onClick={() => aprobarMedico(medico.id)}
                                >
                                    Aprobar
                                </button>
                            )}
                        </td>
                    </tr>
                ))}
                </tbody>
            </table>
        </div>
    );
};

export default AdminPanel;
