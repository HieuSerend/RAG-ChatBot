import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { register } from "../../services/authAPI";


export default function SignupForm() {
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const isValid = password.length >= 6 && password === confirmPassword;
    const [error, setError] = useState("");
    const navigate = useNavigate();


    async function handleSubmit(e: React.FormEvent) {
        e.preventDefault();

        try {
            await register({ username, password });
            navigate("/auth/login", { replace: true });
        } catch {
            setError("Registration failed");
        }
    }


    return (
        <form onSubmit={handleSubmit} className="flex flex-col gap-5">
            <div className="flex flex-col gap-4">
                <input
                    className="w-full px-4 py-3 rounded-lg bg-slate-950/50 border border-slate-700 text-white placeholder-slate-400 text-sm outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-all duration-200"
                    value={username}
                    onChange={e => setUsername(e.target.value)}
                    placeholder="Username or Email"
                    required
                />
                <input
                    className="w-full px-4 py-3 rounded-lg bg-slate-950/50 border border-slate-700 text-white placeholder-slate-400 text-sm outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-all duration-200"
                    type="password"
                    value={password}
                    onChange={e => setPassword(e.target.value)}
                    placeholder="Enter your password"
                    required
                />
                <input
                    className="w-full px-4 py-3 rounded-lg bg-slate-950/50 border border-slate-700 text-white placeholder-slate-400 text-sm outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-all duration-200"
                    type="password"
                    value={confirmPassword}
                    onChange={e => setConfirmPassword(e.target.value)}
                    placeholder="Confirm Password"
                    required
                />
            </div>

            {
                !isValid &&
                <p className="text-sm text-rose-400 bg-rose-400/10 p-2 rounded border border-rose-400/20">Password must be at least 6 characters and match</p>
            }

            {
                error &&
                <p className="text-sm text-rose-400 bg-rose-400/10 p-2 rounded border border-rose-400/20">{error}</p>
            }

            <button
                type="submit"
                disabled={!isValid}
                className={`w-full px-4 py-3 rounded-lg font-semibold shadow-lg transition-all duration-200 ${!isValid ? 'bg-slate-700 text-slate-400 cursor-not-allowed' : 'bg-gradient-to-r from-blue-600 to-blue-500 text-white shadow-blue-500/20 hover:shadow-blue-500/40 hover:from-blue-500 hover:to-blue-400 active:scale-[0.98]'}`}
            >
                Sign Up
            </button>

            <div className="flex flex-row items-center justify-center space-x-2 text-sm text-slate-300 mt-2">
                <span>Already have an account?</span>
                <button
                    className="text-blue-400 font-semibold hover:text-blue-300 transition-colors"
                    type="button"
                    onClick={() => navigate("/auth/login")}
                >
                    Login
                </button>
            </div>
        </form>
    );
}