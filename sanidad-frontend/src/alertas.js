import Swal from 'sweetalert2';
import './alertas.css';

const configBase = {
    background: '#1f2235',
    color: '#ffffff',
    buttonsStyling: false,
    customClass: {
        popup: 'swal-popup',
        title: 'swal-title',
        htmlContainer: 'swal-text',
        confirmButton: 'swal-confirm',
        cancelButton: 'swal-cancel',
        actions: 'swal-actions'
    }
};

export const alertaConfirmacion = async ({
                                             titulo = '¿Estás seguro?',
                                             texto = 'Esta acción no se puede deshacer.',
                                             confirmar = 'Sí, continuar',
                                             cancelar = 'Cancelar',
                                             icono = 'warning'
                                         }) => {

    return await Swal.fire({
        ...configBase,
        title: titulo,
        text: texto,
        icon: icono,
        showCancelButton: true,
        confirmButtonText: confirmar,
        cancelButtonText: cancelar
    });
};

export const alertaExito = (
    texto = 'Operación realizada correctamente'
) => {

    return Swal.fire({
        ...configBase,
        title: 'Listo',
        text: texto,
        icon: 'success',

        toast: true,
        position: 'top-end',

        showConfirmButton: false,

        timer: 2200,
        timerProgressBar: true
    });
};

export const alertaError = (
    texto = 'Ocurrió un error'
) => {

    return Swal.fire({
        ...configBase,
        title: 'Error',
        text: texto,
        icon: 'error',

        confirmButtonText: 'Aceptar'
    });
};