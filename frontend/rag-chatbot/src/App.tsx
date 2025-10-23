import { BrowserRouter } from "react-router-dom";
import AppRoutes from "../src/routes";

export default function App() {
  return (
    <BrowserRouter>
      <AppRoutes />
    </BrowserRouter>
  );
}
