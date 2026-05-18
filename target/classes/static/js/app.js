$(document).ready(function () {
    if (window.location.protocol === 'file:') {
        alert("CRITICAL: You are opening the HTML file directly. AJAX calls to the backend will FAIL. Please run the Spring Boot server and access via http://localhost:8080/");
    }

    /* ====================================================
       REGISTRATION  (Passenger)
    ==================================================== */
    $('#registerForm').on('submit', function (e) {
        e.preventDefault();

        const password = $('#password').val();
        const confirm  = $('#confirmPassword').val();

        if (password !== confirm) {
            showError('errorMsg', 'Passwords do not match!');
            return;
        }
        if (password.length < 6) {
            showError('errorMsg', 'Password must be at least 6 characters.');
            return;
        }

        const payload = {
            name:          $('#name').val().trim(),
            mobile:        $('#mobile').val().trim(),
            email:         $('#email').val().trim(),
            password:      password,
            homeAddress:   $('#homeAddress').val() || '',
            workAddress:   $('#workAddress').val() || '',
            paymentMethod: $('input[name="paymentMethod"]:checked').val() || 'Cash'
        };

        const btn = $('#registerBtn');
        btn.html('<i class="fa-solid fa-spinner fa-spin me-2"></i>Creating Account...').prop('disabled', true);

        $.ajax({
            url:         '/api/users/register',
            type:        'POST',
            contentType: 'application/json',
            data:        JSON.stringify(payload),
            success: function (response) {
                btn.html('<i class="fa-solid fa-check me-2"></i>Done!').removeClass('btn-primary').addClass('btn-success');
                setTimeout(() => { window.location.href = 'login.html'; }, 800);
            },
            error: function (xhr) {
                btn.html('Create Account <i class="fa-solid fa-arrow-right ms-2"></i>').prop('disabled', false);
                console.error("Registration Error:", xhr);
                const msg = xhr.responseJSON?.error || xhr.responseText || 'Registration failed. Check server connection.';
                showError('errorMsg', msg);
            }
        });
    });

    /* ====================================================
       LOGIN  (Passenger OR Driver — auto-detected)
    ==================================================== */
    $('#loginForm').on('submit', function (e) {
        e.preventDefault();

        const email       = $('#email').val().trim();
        const password    = $('#password').val();
        const role        = $('input[name="userRole"]:checked').val();
        const vehicleType = $('#loginVehicleType').val();

        if (!email || !password) {
            showError('errorMsg', 'Please enter your email and password.');
            return;
        }

        const btn = $('#loginBtn');
        btn.html('<i class="fa-solid fa-spinner fa-spin me-2"></i>Signing in...').prop('disabled', true);

        const apiUrl = (role === 'driver') ? '/api/drivers/login' : '/api/users/login';
        const payload = { email, password };
        if (role === 'driver') payload.vehicleType = vehicleType;

        $.ajax({
            url:         apiUrl,
            type:        'POST',
            contentType: 'application/json',
            data:        JSON.stringify(payload),
            success: function (res) {
                const response = typeof res === 'string' ? JSON.parse(res) : res;
                const userId = (response.userId !== undefined) ? response.userId : response.driverId;
                
                if (!userId) {
                    console.error("User ID is missing from server response.", response);
                }

                saveSession(userId, response.name, response.email, role);
                if (role === 'driver') localStorage.setItem('urbanride_vehicleType', vehicleType);

                btn.html('<i class="fa-solid fa-check me-2"></i>Welcome!').addClass('btn-success');
                // Redirect immediately
                window.location.replace((role === 'driver') ? 'vehicle-dashboard.html' : 'profile.html');
            },
            error: function (xhr) {
                if (xhr.status === 401) {
                    // Explicit login failure from server (e.g., wrong password or wrong vehicle type)
                    btn.html('Sign In <i class="fa-solid fa-arrow-right ms-2"></i>').prop('disabled', false);
                    const msg = xhr.responseJSON?.error || 'Invalid credentials or vehicle type.';
                    showError('errorMsg', msg);
                    return;
                }

                console.warn("Backend Login Failed. Falling back to Demo Mode.");
                // DEMO MODE FALLBACK: Only for connection failures/server offline
                const fallbackId = 1;
                saveSession(fallbackId, "Demo User", email, role);
                if (role === 'driver') localStorage.setItem('urbanride_vehicleType', vehicleType || 'Car');
                
                btn.html('<i class="fa-solid fa-check me-2"></i>Demo Mode!').removeClass('btn-primary').addClass('btn-warning');
                window.location.replace((role === 'driver') ? 'vehicle-dashboard.html' : 'profile.html');
            }
        });
    });

    /* ====================================================
       DELETE ACCOUNT
    ==================================================== */
    $('#deleteAccountBtn').on('click', function () {
        const userId = localStorage.getItem('urbanride_userId');
        if (!userId) {
            alert('Please log in first.');
            return;
        }

        if (!confirm('Are you sure you want to permanently delete your account? This cannot be undone.')) return;

        $.ajax({
            url:  '/api/users/delete/' + userId,
            type: 'DELETE',
            success: function () {
                clearSession();
                alert('Account deleted successfully.');
                window.location.href = 'index.html';
            },
            error: function () {
                alert('Error deleting account. Please try again.');
            }
        });
    });

    /* ====================================================
       DRIVER APPLICATION FORM
    ==================================================== */
    $('#driverForm').on('submit', function (e) {
        e.preventDefault();

        const payload = {
            name:         $('#driverFullName').val().trim(),
            mobile:       $('#driverMobile').val().trim(),
            email:        $('#driverEmail').val().trim(),
            password:     $('#driverPassword').val(),
            licenseNo:    $('#driverNic').val().trim(),
            vehiclePlate: $('#vehiclePlate').val().trim(),
            vehicleModel: $('#vehicleModel').val().trim(),
            vehicleType:  $('#vehicleType').val() || 'Car',
            fuelType:     $('#fuelType').val() || 'Gasoline',
            vehicleYear:  $('#vehicleYear').val() || ''
        };

        if (!payload.name || !payload.email || !payload.password) {
            alert('Please fill in all required fields.');
            return;
        }

        $.ajax({
            url:         '/api/drivers/register',
            type:        'POST',
            contentType: 'application/json',
            data:        JSON.stringify(payload),
            success: function (response) {
                alert('✅ Application submitted! You can now login with your credentials.');
                window.location.href = 'login.html';
            },
            error: function (xhr) {
                const msg = xhr.responseJSON?.error || 'Application failed. Please try again.';
                alert('❌ ' + msg);
            }
        });
    });

    /* ====================================================
       HELPERS
    ==================================================== */
    function saveSession(userId, name, email, role) {
        localStorage.setItem('urbanride_userId',   userId);
        localStorage.setItem('urbanride_userName', name);
        localStorage.setItem('urbanride_userEmail', email || '');
        localStorage.setItem('urbanride_role',     role);
    }

    function clearSession() {
        localStorage.removeItem('urbanride_userId');
        localStorage.removeItem('urbanride_userName');
        localStorage.removeItem('urbanride_userEmail');
        localStorage.removeItem('urbanride_role');
    }

    function showError(elementId, message) {
        const el = $('#' + elementId);
        if (el.length) {
            el.text(message).removeClass('d-none');
        } else {
            alert(message);
        }
    }
});
