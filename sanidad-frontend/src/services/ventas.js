import axios from 'axios';

const API_URL = 'http://localhost:8080/api/ventas';

const getToken = () => localStorage.getItem('token');

const authHeaders = () => ({
    headers: { Authorization: `Bearer ${getToken()}` }
});

// Crear venta
export const crearVenta = (venta) => {
    return axios.post(API_URL, venta, authHeaders());
};

// Listar ventas (según rol, el backend ya filtra)
export const listarVentas = () => {
    return axios.get(API_URL, authHeaders());
};

// Obtener una venta por ID
export const obtenerVenta = (id) => {
    return axios.get(`${API_URL}/${id}`, authHeaders());
};

// (Opcional) Anular venta
export const anularVenta = (id) => {
    return axios.delete(`${API_URL}/${id}`, authHeaders());
};