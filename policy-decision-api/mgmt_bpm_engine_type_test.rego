package wks.authz

import future.keywords

test_not_allow_when_not_contain_role_for_user_manager if {
    not allow with input as { 
        "realm_access": { "roles": ["client_case"] },
        "host": "app.wkspower.local",
        "method": "GET",
        "path": "bpm-engine-type"
    }
}

test_allow_when_contain_role_for_user_manager if {
    allow with input as { 
        "realm_access": { "roles": ["mgmt_bpm_engine_type"] },
        "host": "app.wkspower.local",
        "method": "GET",
        "path": "bpm-engine-type"
    }
}