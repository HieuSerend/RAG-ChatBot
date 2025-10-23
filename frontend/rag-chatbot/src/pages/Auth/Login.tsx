import AuthCard from "../../components/auth/AuthCard";
import LoginForm from "../../components/auth/LoginForm";

export default function Login() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-[#f2f4f8]">
      <AuthCard title="Đăng nhập">
        <LoginForm />
      </AuthCard>
    </div>
  );
}
