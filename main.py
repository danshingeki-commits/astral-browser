import flet as ft
import urllib.parse

try:
    import flet_webview as fwv
    WebViewControl = fwv.WebView
except Exception:
    WebViewControl = getattr(ft, "WebView", None)

def main(page: ft.Page):
    page.title = "Astral Browser"
    page.theme_mode = ft.ThemeMode.DARK
    page.bgcolor = "#0B0813"
    page.padding = 0
    page.spacing = 0

    url_inicial = "https://duckduckgo.com"

    # Componente WebView principal
    wv = WebViewControl(
        url=url_inicial,
        expand=True,
        on_page_ended=lambda e: actualizar_url_barra(e)
    )

    def actualizar_url_barra(e):
        try:
            if hasattr(e, "data") and e.data:
                txt_url.value = e.data
                page.update()
        except Exception:
            pass

    def navegar(e=None):
        query = txt_url.value.strip() if txt_url.value else ""
        if not query:
            destino = url_inicial
        elif query.startswith("http://") or query.startswith("https://"):
            destino = query
        elif "." in query and " " not in query:
            destino = f"https://{query}"
        else:
            # Búsqueda privada predeterminada en DuckDuckGo
            destino = f"https://duckduckgo.com/?q={urllib.parse.quote(query)}"

        txt_url.value = destino
        wv.url = destino
        page.update()

    def retroceder(e):
        try:
            wv.go_back()
        except Exception:
            pass

    def avanzar(e):
        try:
            wv.go_forward()
        except Exception:
            pass

    def recargar(e):
        try:
            wv.reload()
        except Exception:
            pass

    def quemar_sesion(e):
        try:
            wv.clear_cache()
            wv.clear_local_storage()
        except Exception:
            pass
        
        wv.url = url_inicial
        txt_url.value = ""
        
        page.overlay.append(
            ft.SnackBar(
                content=ft.Row([
                    ft.Icon(ft.Icons.LOCAL_FIRE_DEPARTMENT, color="#EF4444"),
                    ft.Text("🔥 Rastro destruido: Caché y cookies eliminadas.", color="#FFFFFF")
                ]),
                bgcolor="#1E1233",
                open=True
            )
        )
        page.update()

    # Barra de direcciones superior
    txt_url = ft.TextField(
        hint_text="Buscar con DuckDuckGo o escribir URL...",
        bgcolor="#1A132C",
        border_color="#3D295C",
        border_radius=20,
        height=40,
        text_size=12,
        color="#F8FAFC",
        content_padding=ft.padding.symmetric(horizontal=12, vertical=0),
        expand=True,
        on_submit=navegar
    )

    barra_navegacion = ft.Container(
        bgcolor="#110B1F",
        padding=ft.padding.only(top=35, left=6, right=6, bottom=8),
        border=ft.border.only(bottom=ft.BorderSide(1, "#281A42")),
        content=ft.Row([
            ft.IconButton(
                icon=ft.Icons.ARROW_BACK_IOS_NEW_ROUNDED,
                icon_size=16,
                icon_color="#C084FC",
                tooltip="Atrás",
                on_click=retroceder
            ),
            ft.IconButton(
                icon=ft.Icons.ARROW_FORWARD_IOS_ROUNDED,
                icon_size=16,
                icon_color="#C084FC",
                tooltip="Adelante",
                on_click=avanzar
            ),
            ft.IconButton(
                icon=ft.Icons.REFRESH_ROUNDED,
                icon_size=18,
                icon_color="#94A3B8",
                tooltip="Recargar",
                on_click=recargar
            ),
            txt_url,
            ft.IconButton(
                icon=ft.Icons.SEARCH_ROUNDED,
                icon_size=18,
                icon_color="#38BDF8",
                tooltip="Ir",
                on_click=navegar
            ),
            ft.IconButton(
                icon=ft.Icons.LOCAL_FIRE_DEPARTMENT_ROUNDED,
                icon_size=20,
                icon_color="#EF4444",
                tooltip="Destruir rastro y caché",
                on_click=quemar_sesion
            ),
        ], spacing=2, alignment=ft.MainAxisAlignment.CENTER)
    )

    page.add(
        ft.Column([
            barra_navegacion,
            wv
        ], expand=True, spacing=0)
    )

if __name__ == "__main__":
    ft.app(target=main)
  
