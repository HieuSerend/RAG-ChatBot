import React from "react";
import { Routes, Route, Navigate } from "react-router-dom";
import Home from "../pages/Home/Home";
import Login from "../pages/Auth/Login";
import ProtectedRoute from "./ProtectedRoute";
import Signup from "../pages/Auth/Signup";

export default function AppRoutes() {
  return (
    <Routes>
      <Route path="/auth/login" element={<Login />} />
      <Route path="/auth/signup" element={<Signup />} />

      <Route
        path="/"
        element={
          // <ProtectedRoute>
          //   <Home />
          // </ProtectedRoute>
          <Home />
        }
      />

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
