import  AuthCard  from "../../components/auth/AuthCard";
import SignupForm from "../../components/auth/SignupForm";

export default function Signup() {
    return (
        <div className="min-h-screen flex items-center justify-center bg-[#f2f4f8]">
              <AuthCard title="Đăng ký">
                <SignupForm />
              </AuthCard>
        </div>
    )
}
