import { createContext, useState, useContext, useEffect } from 'react';
import { login as apiLogin } from '../services/auth';
import { jwtDecode } from 'jwt-decode';

const AuthContext = createContext();

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) throw new Error('useAuth debe usarse dentro de AuthProvider');
    return context;
};

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    // Función para quitar el prefijo ROLE_ si existe
    const normalizarRol = (rol) => {
        if (!rol) return null;
        return rol.startsWith('ROLE_') ? rol.substring(5) : rol;
    };

    useEffect(() => {
        const token = localStorage.getItem('token');
        if (token) {
            try {
                const decoded = jwtDecode(token);
                const rolRaw = decoded.rol || decoded.authorities?.[0]?.authority;
                const rol = normalizarRol(rolRaw);
                const id = decoded.id; // ⬅️ NUEVO: extraemos el id del token

                setUser({
                    token,
                    id,                // ⬅️ NUEVO
                    username: decoded.sub,
                    rol
                });
            } catch (error) {
                console.error('Error decodificando token:', error);
                localStorage.removeItem('token');
            }
        }
        setLoading(false);
    }, []);

    const login = async (username, password) => {
        try {
            const response = await apiLogin(username, password);
            const { token } = response.data;
            localStorage.setItem('token', token);

            const decoded = jwtDecode(token);
            const rolRaw = decoded.rol || decoded.authorities?.[0]?.authority;
            const rol = normalizarRol(rolRaw);
            const id = decoded.id; // ⬅️ NUEVO

            setUser({
                token,
                id,                  // ⬅️ NUEVO
                username: decoded.sub,
                rol
            });
            return { success: true };
        } catch (error) {
            return {
                success: false,
                message: error.response?.data?.message || 'Error al iniciar sesión'
            };
        }
    };

    const logout = () => {
        localStorage.removeItem('token');
        setUser(null);
    };

    return (
        <AuthContext.Provider value={{ user, loading, login, logout }}>
            {children}
        </AuthContext.Provider>
    );
};