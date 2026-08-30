import {
  BrowserRouter,
  Navigate,
  Route,
  Routes,
} from "react-router-dom";

import { AuthProvider } from "@/auth/AuthProvider";
import { ProtectedRoute } from "@/auth/ProtectedRoute";
import { CreateDocumentPage } from "@/pages/CreateDocumentPage";
import { DocumentDetailPage } from "@/pages/DocumentDetailPage";
import { DocumentsPage } from "@/pages/DocumentsPage";
import { LoginPage } from "@/pages/LoginPage";

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route
            path="/login"
            element={<LoginPage />}
          />

          <Route
            element={<ProtectedRoute />}
          >
            <Route
              path="/documents"
              element={<DocumentsPage />}
            />
            <Route
              path="/documents/new"
              element={<CreateDocumentPage />}
            />
            <Route
              path="/documents/:id"
              element={<DocumentDetailPage />}
            />
          </Route>

          <Route
            path="/"
            element={
              <Navigate
                to="/documents"
                replace
              />
            }
          />

          <Route
            path="*"
            element={
              <Navigate
                to="/documents"
                replace
              />
            }
          />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
