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
    <form className="flex flex-col gap-3 mt-2" onSubmit={handleSubmit}>
      <input
        className="px-3 py-2 rounded-lg border border-slate-200 text-sm outline-none focus:outline-none focus:ring-4 focus:ring-indigo-100"
        type="text"
        placeholder="Username"
        value={username}
        onChange={(e) => setUsername(e.target.value)}
        required
      />
      <input
        className="px-3 py-2 rounded-lg border border-slate-200 text-sm outline-none focus:outline-none focus:ring-4 focus:ring-indigo-100"
        type="password"
        placeholder="Password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        required
      />
      <button className="px-3 py-2 rounded-lg bg-indigo-600 text-white font-semibold active:opacity-60 transition-opacity duration-150" type="submit" disabled={loading}>
        {loading ? "Đang xử lý..." : "Đăng nhập"}
      </button>
      <div className="flex flex-row space-x-2 justify-center">
        <button className="text-blue-600 font-semibold active:opacity-60 transition-opacity duration-150" type="button" onClick={() => navigate("/auth/signup")} disabled={loading}>
            Đăng ký
        </button>
        <p> nếu chưa có tài khoản</p>
      </div>
      {message && <p className="mt-2 text-sm text-slate-600">{message}</p>}
    </form>
  );
}
