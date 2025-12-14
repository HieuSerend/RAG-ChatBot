import AuthCard from "../../components/auth/AuthCard";
import SignupForm from "../../components/auth/SignupForm";

export default function Signup() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-950 relative overflow-hidden">
      {/* Background Image */}
      <div
        className="absolute inset-0 z-0 bg-cover bg-center bg-no-repeat"
        style={{ backgroundImage: "url('/src/assets/bg_money_dark.png')" }}
      />
      {/* Heavy Dark Overlay */}
      <div className="absolute inset-0 z-10 bg-slate-900/80 backdrop-blur-[2px]" />

      {/* Content Container */}
      <div className="relative z-20">
        <AuthCard title="Sign Up">
          <SignupForm />
        </AuthCard>
      </div>
    </div>
  )
}
