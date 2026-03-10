import React, { useState } from 'react';
import '../../css/stylesheet.css';
import { useNavigate } from 'react-router-dom';

const RegisterDoctor = () => {
    const [form, setForm] = useState({
        nombre: '',
        clave: '',
        confirmClave: '',
        especialidad: '',
        ciudad: '',
        costoConsulta: '',
        frecuencia: '',
        instalacion: ''
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
            const res = await fetch('http://localhost:8080/auth/register/medico', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    usuario: {
                        nombre: form.nombre,
                        clave: form.clave
                    },
                    especialidad: form.especialidad,
                    ciudad: form.ciudad,
                    costoConsulta: parseInt(form.costoConsulta),
                    frecuencia: parseInt(form.frecuencia),
                    instalacion: form.instalacion
                })
            });

            if (!res.ok) {
                const message = await res.text();
                setError(message || 'Error al registrar médico');
                return;
            }

            alert('Registro exitoso. Espere aprobación.');
            navigate('/login');
        } catch (err) {
            setError('Error de conexión con el servidor');
        }
    };

    return (
        <div className="login-container-auth">
            <div className="login-box-auth">
                <h2>Doctor Registration</h2>
                <form onSubmit={handleSubmit}>
                    <div className="input-group-auth">
                        <i className="bi bi-person-fill"></i>
                        <input type="text" name="nombre" placeholder="Full Name"
                               value={form.nombre} onChange={handleChange} required />
                    </div>

                    <div className="input-group-auth">
                        <i className="bi bi-key-fill"></i>
                        <input type="password" name="clave" placeholder="Password"
                               value={form.clave} onChange={handleChange} required />
                    </div>

                    <div className="input-group-auth">
                        <i className="bi bi-key-fill"></i>
                        <input type="password" name="confirmClave" placeholder="Confirm Password"
                               value={form.confirmClave} onChange={handleChange} required />
                    </div>

                    <div className="input-group-auth">
                        <i className="bi bi-briefcase-fill"></i>
                        <input type="text" name="especialidad" placeholder="Specialty"
                               value={form.especialidad} onChange={handleChange} required />
                    </div>

                    <div className="input-group-auth">
                        <i className="bi bi-geo-alt-fill"></i>
                        <input type="text" name="ciudad" placeholder="City"
                               value={form.ciudad} onChange={handleChange} required />
                    </div>

                    <div className="input-group-auth">
                        <i className="bi bi-cash"></i>
                        <input type="number" name="costoConsulta" placeholder="Consultation Cost"
                               value={form.costoConsulta} onChange={handleChange} required min="1" />
                    </div>

                    <div className="input-group-auth">
                        <i className="bi bi-clock-fill"></i>
                        <input type="number" name="frecuencia" placeholder="Frequency (minutes)"
                               value={form.frecuencia} onChange={handleChange} required min="1" />
                    </div>

                    <div className="input-group-auth">
                        <i className="bi bi-building"></i>
                        <input type="text" name="instalacion" placeholder="Facility Name"
                               value={form.instalacion} onChange={handleChange} required />
                    </div>

                    <button type="submit" className="login-btn-auth">Register</button>
                </form>

                {error && <div className="error-message-auth">{error}</div>}
            </div>
        </div>
    );
};

export default RegisterDoctor;
