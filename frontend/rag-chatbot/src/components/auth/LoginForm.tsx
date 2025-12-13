import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { login } from "../../services/authAPI";

export default function LoginForm() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setMessage(null);
    setLoading(true);
    try {
      const res = await login({ username, password });
      if (res?.data?.valid) {
        // login success -> navigate to home (protected)
        navigate("/", { replace: true });
      } else {
        setMessage("Đăng nhập không thành công");
      }
    } catch (err: unknown) {
      const messageText = (err as unknown as { response?: { data?: { message?: string } } })?.response?.data?.message || "Lỗi khi đăng nhập";
      setMessage(messageText);
    } finally {
      setLoading(false);
    }
  };

  return (
    <form className="flex flex-col gap-5 mt-2" onSubmit={handleSubmit}>
      <div className="flex flex-col gap-4">
        <input
          className="w-full px-4 py-3 rounded-lg bg-slate-950/50 border border-slate-700 text-white placeholder-slate-400 text-sm outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-all duration-200"
          type="text"
          placeholder="Username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          required
        />
        <input
          className="w-full px-4 py-3 rounded-lg bg-slate-950/50 border border-slate-700 text-white placeholder-slate-400 text-sm outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-all duration-200"
          type="password"
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
      </div>

      <button
        className="w-full px-4 py-3 rounded-lg bg-gradient-to-r from-blue-600 to-blue-500 text-white font-semibold shadow-lg shadow-blue-500/20 hover:shadow-blue-500/40 hover:from-blue-500 hover:to-blue-400 active:scale-[0.98] transition-all duration-200"
        type="submit"
        disabled={loading}
      >
        {loading ? "Đang xử lý..." : "Đăng nhập"}
      </button>

      <div className="flex flex-row items-center justify-center space-x-2 text-sm text-slate-300">
        <span>Chưa có tài khoản?</span>
        <button
          className="text-blue-400 font-semibold hover:text-blue-300 transition-colors"
          type="button"
          onClick={() => navigate("/auth/signup")}
          disabled={loading}
        >
          Đăng ký ngay
        </button>
      </div>
      {message && <p className="mt-2 text-sm text-red-400 bg-red-400/10 p-2 rounded border border-red-400/20">{message}</p>}
    </form>
  );
}
