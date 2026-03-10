import React from 'react';
import '../../css/stylesheet.css';
import { Link } from 'react-router-dom';

const RegisterType = () => {
    return (
        <div className="login-container-auth">
            <div className="login-box-auth">
                <h2>Select Your Registration Type</h2>
                <br /><br />
                <Link to="/register/patient" className="login-btn-auth">Patient</Link>
                &nbsp;
                <Link to="/register/doctor" className="login-btn-auth">Doctor</Link>
            </div>
        </div>
    );
};

export default RegisterType;
