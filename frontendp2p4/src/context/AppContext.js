import { createContext, useState, useEffect } from 'react';
import { jwtDecode } from 'jwt-decode';

export const AppContext = createContext();

export const AppProvider = ({ children }) => {
    const [usuario, setUsuario] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const token = localStorage.getItem('token');
        const rol = localStorage.getItem('rol');
        const nombre = localStorage.getItem('nombre');

        if (token && rol && nombre) {
            try {
                const decoded = jwtDecode(token);
                const now = Date.now() / 1000;
                if (decoded.exp < now) {
                    localStorage.clear();
                    setUsuario(null);
                } else {
                    setUsuario({ token, rol, nombre });
                }
            } catch (e) {
                localStorage.clear();
                setUsuario(null);
            }
        } else {
            setUsuario(null);
        }
        setLoading(false);
    }, []);


    const login = ({ token, rol, nombre }) => {
        localStorage.setItem('token', token);
        localStorage.setItem('rol', rol);
        localStorage.setItem('nombre', nombre);
        setUsuario({ token, rol, nombre });
    };

    const logout = () => {
        localStorage.clear();
        setUsuario(null);
    };

    return (
        <AppContext.Provider value={{ usuario, login, logout, loading }}>
            {children}
        </AppContext.Provider>
    );
};
