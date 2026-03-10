import React, { useState } from 'react';
import '../../css/stylesheet.css';
import { useNavigate } from 'react-router-dom';

const RegisterPatient = () => {
    const [form, setForm] = useState({
        nombre: '',
        clave: '',
        confirmClave: '',
        historialMedico: '',
        alergias: '',
        rol: 'PACIENTE'
    });

    const [error, setError] = useState(null);
    const navigate = useNavigate();

    const handleChange = (e) => {
        const { name, value } = e.target;
        setForm((prev) => ({ ...prev, [name]: value }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (form.clave !== form.confirmClave) {
            setError('Passwords do not match!');
            return;
        }

        try {
            const res = await fetch('http://localhost:8080/auth/register/paciente', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    nombre: form.nombre,
                    clave: form.clave,
                    rol: form.rol
                }),
            });

            if (!res.ok) {
                const message = await res.text();
                setError(message || 'Error al registrar');
                return;
            }

            navigate('/login'); // Redirige al login
        } catch (err) {
            setError('Error de conexión con el servidor');
        }
    };

    return (
        <div className="login-container-auth">
            <div className="login-box-auth">
                <h2>Patient Registration</h2>
                <form onSubmit={handleSubmit}>
                    <div className="input-group-auth">
                        <i className="bi bi-person-fill"></i>
                        <input
                            type="text"
                            name="nombre"
                            placeholder="Full Name"
                            value={form.nombre}
                            onChange={handleChange}
                            required
                            pattern="[^\s]+"
                            title="No spaces allowed"
                        />
                    </div>

                    <div className="input-group-auth">
                        <i className="bi bi-key-fill"></i>
                        <input
                            type="password"
                            name="clave"
                            placeholder="Password"
                            value={form.clave}
                            onChange={handleChange}
                            required
                        />
                    </div>

                    <div className="input-group-auth">
                        <i className="bi bi-key-fill"></i>
                        <input
                            type="password"
                            name="confirmClave"
                            placeholder="Confirm Password"
                            value={form.confirmClave}
                            onChange={handleChange}
                            required
                        />
                    </div>

                    <div className="input-group-auth">
                        <i className="bi bi-calendar-check"></i>
                        <input
                            type="text"
                            name="historialMedico"
                            placeholder="Medical History"
                            value={form.historialMedico}
                            onChange={handleChange}
                            required
                        />
                    </div>

                    <div className="input-group-auth">
                        <i className="bi bi-heart-pulse"></i>
                        <input
                            type="text"
                            name="alergias"
                            placeholder="Allergies"
                            value={form.alergias}
                            onChange={handleChange}
                            required
                        />
                    </div>

                    <button type="submit" className="login-btn-auth">Register</button>
                </form>

                {error && <div className="error-message-auth">{error}</div>}
            </div>
        </div>
    );
};

export default RegisterPatient;
