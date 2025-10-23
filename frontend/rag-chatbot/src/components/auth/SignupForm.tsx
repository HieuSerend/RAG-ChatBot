import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { register } from "../../services/authAPI";


export default function SignupForm() {
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const isValid = password.length >=6 && password === confirmPassword;
    const [error, setError] = useState("");
    const navigate = useNavigate();


    async function handleSubmit(e: React.FormEvent) {
        e.preventDefault();

        try {
            await register({ username, password });
            navigate("/auth/login", { replace: true });
        } catch (err) {
            setError("Đăng ký thất bại");
        }
    }


    return (
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <input className="px-3 py-2 rounded-lg border border-slate-200 text-sm" value={username} onChange={e=>setUsername(e.target.value)} placeholder="Username or Email" required />
        <input 
            className="px-3 py-2 rounded-lg border border-slate-200 text-sm"
            type="password" 
            value={password} 
            onChange={e=>setPassword(e.target.value)} 
            placeholder="Enter your password" required 
        />
        <input 
            className="px-3 py-2 rounded-lg border border-slate-200 text-sm"
            type="password" 
            value={confirmPassword} 
            onChange={e=>setConfirmPassword(e.target.value)} 
            placeholder="Confirm Password" required 
        />

        {
            !isValid && 
            <p className="text-sm text-red-500">Mật khẩu phải nhiều hơn 6 ký tự và khớp nhau</p>
        }

        {
            error && 
            <p className="text-sm text-red-500">{error}</p>
        }

    <button type="submit" disabled={!isValid} className="bg-blue-600 text-white p-2 rounded-lg">Đăng ký</button>
        </form>
    );
}