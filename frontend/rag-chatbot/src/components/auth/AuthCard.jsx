import React from "react";
import "../../styles/auth/auth.css";

const AuthCard = ({ title, children }) => {
  return (
    <div className="auth-card">
      <h2>{title}</h2>
      {children}
    </div>
  );
};

export default AuthCard;
