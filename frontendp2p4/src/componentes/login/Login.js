import React, { useState, useContext } from 'react';
import usuario from '../../imagenes/usuario.png';
import '../../css/stylesheet.css';
import { Link, useNavigate } from 'react-router-dom';
import { AppContext } from '../../context/AppContext';

const Login = () => {
    const [userId, setUserId] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState(null);

    const { login } = useContext(AppContext);
    const navigate = useNavigate();

    const handleLogin = async (e) => {
        e.preventDefault();

        try {
            const res = await fetch('http://localhost:8080/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ nombre: userId, clave: password }),
            });

            if (!res.ok) {
                const message = await res.text();
                setError(message || 'Credenciales incorrectas');
                return;
            }

            const data = await res.json();

            login({ token: data.token, rol: data.rol, nombre: data.nombre });
            localStorage.setItem("usuario", JSON.stringify({ nombre: data.nombre, rol: data.rol }));

            const savedUrl = localStorage.getItem("previourl");
            localStorage.removeItem("previourl");

            if (savedUrl && data.rol === 'PACIENTE') {
                navigate(savedUrl);
            } else if (data.rol === 'MEDICO') {
                navigate('/medico/MedicoPanel');
            } else if (data.rol === 'ADMINISTRADOR') {
                navigate('/admin/AdminPanel');
            } else {
                navigate('/');
            }

        } catch (err) {
            setError('Error de conexión con el servidor');
        }
    };

    return (
        <div className="login-container-auth">
            <div className="login-box-auth">
                <h2>Login</h2><br />
                <div className="user-avatar-auth">
                    <img src={usuario} alt="User Avatar" />
                </div>
                <form onSubmit={handleLogin}>
                    <div className="input-group-auth">
                        <i className="bi bi-person-fill"></i>
                        <input
                            type="text"
                            placeholder="User id"
                            value={userId}
                            onChange={(e) => setUserId(e.target.value)}
                            required
                        />
                    </div>
                    <div className="input-group-auth">
                        <i className="bi bi-key-fill"></i>
                        <input
                            type="password"
                            placeholder="User Password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                        />
                    </div>
                    <button type="submit" className="login-btn-auth">Log in</button>
                </form>
                {error && <p className="error-message-auth">{error}</p>}
                <br />
                <p>Don't have an account? <Link to="/register" className="register-link-auth">Register</Link></p>
            </div>
        </div>
    );
};

export default Login;
