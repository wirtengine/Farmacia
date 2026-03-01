import React, { createContext, useState, useEffect } from 'react';
import authService from '../services/authService';

export const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (authService.isAuthenticated()) {
            setUser({
                token: authService.getToken(),
                rol: authService.getRol(),
                nombre: authService.getNombre()
            });
        }
        setLoading(false);
    }, []);

    const login = async (username, password) => {
        try {
            const response = await authService.login(username, password);
            const { token, rol, nombre } = response.data;
            authService.setAuthData(token, rol, nombre);
            setUser({ token, rol, nombre });
            return { success: true };
        } catch (error) {
            return { success: false, message: error.response?.data || 'Error en el login' };
        }
    };

    const logout = () => {
        authService.logout();
        setUser(null);
    };

    return (
        <AuthContext.Provider value={{ user, login, logout, loading }}>
            {children}
        </AuthContext.Provider>
    );
};